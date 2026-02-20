package org.example.project.ble.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.datastore.preferences.core.Preferences
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.example.project.R
import org.example.project.ble.TpmsBluetoothManager
import org.example.project.ble.TpmsState
import org.example.project.ble.TyreWheelPosition
import org.example.project.widget.TyrePressureWidget
import org.example.project.widget.TyreWidgetKeys
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TpmsForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var updateJob: Job? = null

    companion object {
        const val CHANNEL_ID = "TpmsServiceChannel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, TpmsForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        startMonitoring()
    }

    private fun startForegroundService() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TyreGuard Active")
            .setContentText("Monitoring tyre pressure in background")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this exists
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TPMS Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startMonitoring() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            val manager = TpmsBluetoothManager.getInstance(applicationContext)
            
            manager.state.collect { state ->
                updateWidgetData(state)
            }
        }
    }

    private suspend fun updateWidgetData(state: TpmsState) {
        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val glanceId = GlanceAppWidgetManager(this)
            .getGlanceIds(TyrePressureWidget::class.java)
            .firstOrNull()

        if (glanceId != null) {
            updateAppWidgetState(this, glanceId) { prefs ->
                prefs[TyreWidgetKeys.LAST_UPDATED] = timestamp
                
                val positionData = state.getAllPositionData()

                positionData[TyreWheelPosition.FRONT_LEFT]?.let { 
                    prefs[TyreWidgetKeys.PSI_FL] = it.pressurePsi 
                }
                positionData[TyreWheelPosition.FRONT_RIGHT]?.let { 
                    prefs[TyreWidgetKeys.PSI_FR] = it.pressurePsi 
                }
                positionData[TyreWheelPosition.REAR_LEFT]?.let { 
                    prefs[TyreWidgetKeys.PSI_RL] = it.pressurePsi 
                }
                positionData[TyreWheelPosition.REAR_RIGHT]?.let { 
                    prefs[TyreWidgetKeys.PSI_RR] = it.pressurePsi 
                }
            }
            TyrePressureWidget().update(this, glanceId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }
}
