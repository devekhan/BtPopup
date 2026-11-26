package com.afroz.btpopup

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class DeviceKind { NOTHING_HEADPHONE, HEADPHONES, EARBUDS, SPEAKER, OTHER }

data class PopupInfo(
    val name: String,
    val kind: DeviceKind,
    val batteryPercent: Int? = null,
)

object DeviceClassifier {
    fun kindOf(name: String?, btClass: BluetoothClass?): DeviceKind {
        val n = name?.lowercase().orEmpty()
        if ("nothing" in n && "headphone" in n) return DeviceKind.NOTHING_HEADPHONE
        if (listOf("buds", "pods", "earbud", "earphone", "ear (").any { it in n }) return DeviceKind.EARBUDS
        return when (btClass?.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceKind.HEADPHONES
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
            BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> DeviceKind.SPEAKER
            else -> DeviceKind.OTHER
        }
    }

    fun isAudio(btClass: BluetoothClass?): Boolean =
        btClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
}

object Prefs {
    private const val FILE = "btpopup"
    fun enabled(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("enabled", false)
    fun setEnabled(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("enabled", v).apply()
}

fun hasBtPermission(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

@Suppress("DEPRECATION")
fun Intent.bluetoothDevice(): BluetoothDevice? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    else getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

/** Device name, safe against missing permission. */
fun BluetoothDevice.safeName(): String =
    (try { name } catch (_: SecurityException) { null }) ?: "Bluetooth device"

/**
 * Battery level is a hidden API on most Android versions, so this is best effort:
 * returns null when the system doesn't expose it for this device.
 */
fun BluetoothDevice.batteryOrNull(): Int? = try {
    val v = BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(this) as Int
    if (v in 0..100) v else null
} catch (_: Throwable) { null }
