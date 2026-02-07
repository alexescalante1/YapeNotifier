package com.example.yapenotifier.data.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.yapenotifier.di.ServiceEntryPoint
import com.example.yapenotifier.domain.model.CapturedEvent
import com.example.yapenotifier.domain.repository.EventRepository
import com.example.yapenotifier.domain.repository.SettingsRepository
import com.example.yapenotifier.util.Constants
import com.example.yapenotifier.widget.YapeWidget
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationMonitorService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val eventRepository: EventRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
            .eventRepository()
    }

    private val settingsRepository: SettingsRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
            .settingsRepository()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        Log.d(TAG, "Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        Log.w(TAG, "Listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: Bundle.EMPTY
        val contentText = extractNotificationText(extras)

        serviceScope.launch {
            // Ignorar si el servicio esta desactivado
            if (!settingsRepository.isServiceEnabledOnce()) return@launch

            // Single DataStore read for all settings
            val snapshot = settingsRepository.getSettingsSnapshot()

            // Filter by package BEFORE any writes
            if (!snapshot.captureAll && !snapshot.packages.contains(packageName)) return@launch

            // Only write lastSeen for relevant packages
            settingsRepository.setLastSeen(packageName, contentText)

            if (contentText.isBlank()) return@launch
            if (!snapshot.captureAll && !isValidYapeEvent(contentText)) return@launch
            if (Dedup.shouldSkip(sbn.key, sbn.postTime, contentText)) {
                Log.d(TAG, "Notification skipped by dedup: ${sbn.key}")
                return@launch
            }

            val amount = extractAmount(contentText)
            val timeText = extractTime(contentText)

            if (snapshot.captureAll) {
                eventRepository.appendEvent(
                    CapturedEvent(
                        amount = amount,
                        time = timeText,
                        text = contentText,
                        timestamp = System.currentTimeMillis(),
                        smsSent = false,
                        packageName = packageName
                    )
                )
                Log.d(TAG, "Captured (test mode): $contentText")
                YapeWidget.updateAllWidgets(this@NotificationMonitorService)
                return@launch
            }

            var smsSent = false
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                this@NotificationMonitorService, android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasSmsPermission) {
                Log.w(TAG, "SMS permission not granted, skipping send")
            } else if (snapshot.numbers.isEmpty()) {
                Log.w(TAG, "No SMS numbers configured")
            } else {
                val message = buildSmsMessage(contentText, amount, timeText)
                smsSent = true
                snapshot.numbers.forEach { number ->
                    if (!sendSms(number, message)) smsSent = false
                    else Log.d(TAG, "SMS sent to $number")
                }
            }

            eventRepository.appendEvent(
                CapturedEvent(
                    amount = if (amount.isBlank()) "S/ ?" else amount,
                    time = timeText,
                    text = contentText,
                    timestamp = System.currentTimeMillis(),
                    smsSent = smsSent,
                    packageName = packageName
                )
            )
            YapeWidget.updateAllWidgets(this@NotificationMonitorService)
        }
    }

    private fun extractNotificationText(extras: Bundle): String {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        if (bigText.isNotBlank()) return bigText
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) return text
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isNotBlank()) return title
        val summary = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim().orEmpty()
        if (summary.isNotBlank()) return summary
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        return subText
    }

    private fun isValidYapeEvent(text: String): Boolean {
        val normalized = text.lowercase(Locale.getDefault())
        val keywordMatch = Constants.YAPE_KEYWORDS.any { normalized.contains(it) }
        val hasAmount = extractAmount(text).isNotBlank()
        return hasAmount || keywordMatch
    }

    private fun extractAmount(text: String): String {
        val match = Constants.AMOUNT_REGEX.find(text) ?: return ""
        val amount = match.groupValues[2].replace(",", ".")
        return "S/ $amount"
    }

    private fun extractTime(text: String): String {
        val match = Constants.TIME_REGEX.find(text) ?: return ""
        return match.value
    }

    private fun buildSmsMessage(contentText: String, amount: String, time: String): String {
        if (contentText.isNotBlank()) return contentText.trim()
        val amountPart = if (amount.isNotBlank()) amount else "S/ ?"
        return if (time.isNotBlank()) {
            "Yape recibido: $amountPart. Hora: $time."
        } else {
            "Yape recibido: $amountPart."
        }
    }

    private fun sendSms(number: String, message: String): Boolean {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(number, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(number, null, message, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $number", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isConnected.value = false
        serviceScope.cancel()
    }

    private object Dedup {
        private const val MAX_ENTRIES = 10
        private data class Entry(val postTime: Long, val text: String, val seenAt: Long)
        private val recentKeys = LinkedHashMap<String, Entry>()

        @Synchronized
        fun shouldSkip(key: String, postTime: Long, text: String): Boolean {
            val now = System.currentTimeMillis()

            // Purge expired entries
            val iter = recentKeys.entries.iterator()
            while (iter.hasNext()) {
                if ((now - iter.next().value.seenAt) > Constants.DEDUP_WINDOW_MS) iter.remove()
            }

            // Check by key
            recentKeys[key]?.let { return true }

            // Check by text + postTime combo
            if (recentKeys.values.any { it.text == text && it.postTime == postTime }) return true

            // Record this notification
            recentKeys[key] = Entry(postTime, text, now)
            while (recentKeys.size > MAX_ENTRIES) {
                recentKeys.entries.iterator().let { i -> i.next(); i.remove() }
            }

            return false
        }
    }

    companion object {
        private const val TAG = "YapeNotifier"
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected
    }
}
