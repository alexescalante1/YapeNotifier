package com.example.yapenotifier.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.yapenotifier.MainActivity
import com.example.yapenotifier.data.service.KeepAliveService
import com.example.yapenotifier.di.ServiceEntryPoint
import com.example.yapenotifier.domain.model.CapturedEvent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class YapeWidget : GlanceAppWidget() {

    // Usar estado propio de Glance para visual inmediato
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ServiceEntryPoint::class.java
        )
        val events = entryPoint.eventRepository().recentEventsFlow(10).first()

        // Sincronizar estado de Glance con DataStore al renderizar
        val dsEnabled = entryPoint.settingsRepository().isServiceEnabledOnce()
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply {
                this[KEY_SERVICE_RUNNING] = dsEnabled
            }
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val serviceRunning = prefs[KEY_SERVICE_RUNNING] ?: false

            GlanceTheme {
                WidgetContent(serviceRunning, events)
            }
        }
    }

    companion object {
        val KEY_SERVICE_RUNNING = booleanPreferencesKey("service_running")

        /**
         * Refresca todos los widgets. Si se pasa serviceEnabled, se escribe
         * directamente al estado Glance (no depende de DataStore).
         */
        suspend fun updateAllWidgets(context: Context, serviceEnabled: Boolean? = null) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(YapeWidget::class.java)
            ids.forEach { id ->
                if (serviceEnabled != null) {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[KEY_SERVICE_RUNNING] = serviceEnabled
                        }
                    }
                }
                YapeWidget().update(context, id)
            }
        }
    }
}

private val Purple = Color(0xFF742D8B)
private val Green = Color(0xFF4CAF50)
private val Red = Color(0xFFCF6679)
private val TextWhite = Color(0xFFFFFFFF)

@Composable
private fun WidgetContent(serviceRunning: Boolean, events: List<CapturedEvent>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YapeNotifier",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>())
            )
            Box(
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .background(if (serviceRunning) Green else Red)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .clickable(actionRunCallback<ToggleServiceAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (serviceRunning) "● ON" else "● OFF",
                    style = TextStyle(
                        color = ColorProvider(TextWhite),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (events.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            LazyColumn {
                items(events) { event ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (event.smsSent) "✓" else "✗",
                            style = TextStyle(
                                color = ColorProvider(if (event.smsSent) Green else Red),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = event.text.ifBlank { event.amount },
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = dateFormat.format(Date(event.timestamp)),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class ToggleServiceAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext, ServiceEntryPoint::class.java
            )

            // 1. Leer estado actual e invertirlo
            val currentlyEnabled = entryPoint.settingsRepository().isServiceEnabledOnce()
            val enable = !currentlyEnabled
            Log.d(TAG, "Widget toggle: was=$currentlyEnabled, now=$enable")

            // 2. Iniciar o detener servicio
            val serviceIntent = Intent(appContext, KeepAliveService::class.java)
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(appContext, serviceIntent)
                } else {
                    appContext.startService(serviceIntent)
                }
            } else {
                appContext.stopService(serviceIntent)
            }

            // 3. Guardar en DataStore (para la app)
            entryPoint.settingsRepository().setServiceEnabled(enable)

            // 4. Actualizar estado Glance directamente + refrescar widget
            updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[YapeWidget.KEY_SERVICE_RUNNING] = enable
                }
            }
            YapeWidget().update(appContext, glanceId)
            Log.d(TAG, "Toggle complete: enabled=$enable")
        } catch (e: Exception) {
            Log.e(TAG, "Toggle failed", e)
        }
    }

    companion object {
        private const val TAG = "YapeWidget"
    }
}
