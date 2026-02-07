package com.example.yapenotifier.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import com.example.yapenotifier.data.datastore.YapeDataStoreKeys
import com.example.yapenotifier.data.datastore.dataStore
import com.example.yapenotifier.data.service.KeepAliveService
import com.example.yapenotifier.data.service.NotificationMonitorService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val serviceEnabled = runBlocking {
            context.dataStore.data.first()[YapeDataStoreKeys.KEY_SERVICE_ENABLED] ?: false
        }
        if (serviceEnabled) {
            val serviceIntent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        val component = ComponentName(context, NotificationMonitorService::class.java)
        NotificationListenerService.requestRebind(component)
    }
}
