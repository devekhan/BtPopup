package com.afroz.btpopup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class BtMonitorService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private val lastShown = HashMap<String, Long>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) != BluetoothProfile.STATE_CONNECTED) return
            val device = intent.bluetoothDevice() ?: return
            val btClass = try { device.bluetoothClass } catch (_: SecurityException) { null }
            if (!DeviceClassifier.isAudio(btClass)) return

            // A2DP and headset profiles both fire; show once per device.
            val now = SystemClock.elapsedRealtime()
            if (now - (lastShown[device.address] ?: 0L) < 10_000L) return
            lastShown[device.address] = now

            val name = device.safeName()
            val kind = DeviceClassifier.kindOf(name, btClass)
            // Give the system a moment so battery info (if any) has been reported.
            main.postDelayed({
                PopupController.show(applicationContext, PopupInfo(name, kind, device.batteryOrNull()))
            }, 800)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Bluetooth watcher", NotificationManager.IMPORTANCE_MIN)
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle("BT Popup is watching for headphones")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)

        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        main.removeCallbacksAndMessages(null)
        PopupController.dismissNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { private const val CHANNEL = "watcher" }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.enabled(context) && hasBtPermission(context)) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, BtMonitorService::class.java))
            } catch (_: Exception) {}
        }
    }
}
