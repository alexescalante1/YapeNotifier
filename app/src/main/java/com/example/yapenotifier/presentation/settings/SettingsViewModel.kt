package com.example.yapenotifier.presentation.settings

import android.Manifest
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yapenotifier.domain.model.SmsContact
import com.example.yapenotifier.domain.model.WatchedPackage
import com.example.yapenotifier.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val contacts = settingsRepository.contactsFlow()
    val watchedPackages = settingsRepository.watchedPackagesFlow()
    val captureAll = settingsRepository.captureAllFlow()
    val lastSeenPackage = settingsRepository.lastSeenPackageFlow()
    val lastSeenText = settingsRepository.lastSeenTextFlow()

    fun addContact(contact: SmsContact) {
        viewModelScope.launch { settingsRepository.addContact(contact) }
    }

    fun removeContact(number: String) {
        viewModelScope.launch { settingsRepository.removeContact(number) }
    }

    fun addWatchedPackage(pkg: WatchedPackage) {
        viewModelScope.launch { settingsRepository.addWatchedPackage(pkg) }
    }

    fun removeWatchedPackage(packageName: String) {
        viewModelScope.launch { settingsRepository.removeWatchedPackage(packageName) }
    }

    fun updateWatchedPackage(oldPackageName: String, updated: WatchedPackage) {
        viewModelScope.launch { settingsRepository.updateWatchedPackage(oldPackageName, updated) }
    }

    fun setCaptureAll(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCaptureAll(enabled) }
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setServiceEnabled(enabled) }
    }

    fun sendTestSmsToContact(context: Context, number: String) {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return@launch
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(number, null, "YapeNotifier: SMS de prueba.", null, null)
            } catch (e: Exception) {
                Log.e("SettingsVM", "Failed to send test SMS to $number", e)
            }
        }
    }
}
