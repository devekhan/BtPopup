package com.afroz.btpopup

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { HomeScreen() }
            }
        }
    }
}

private data class Paired(val name: String, val kind: DeviceKind, val address: String)

@Composable
private fun HomeScreen() {
    val ctx = LocalContext.current
    val activity = ctx as ComponentActivity
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) tick++ }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val btOk = remember(tick) { hasBtPermission(ctx) }
    val overlayOk = remember(tick) { Settings.canDrawOverlays(ctx) }
    var enabled by remember { mutableStateOf(Prefs.enabled(ctx)) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    val paired = remember(tick, btOk) {
        if (!btOk) emptyList<Paired>() else try {
            val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
            (adapter?.bondedDevices ?: emptySet())
                .filter { DeviceClassifier.isAudio(it.bluetoothClass) }
                .map { Paired(it.safeName(), DeviceClassifier.kindOf(it.safeName(), it.bluetoothClass), it.address) }
        } catch (_: Exception) { emptyList<Paired>() }
    }

    fun setMonitoring(on: Boolean) {
        enabled = on
        Prefs.setEnabled(ctx, on)
        val intent = Intent(ctx, BtMonitorService::class.java)
        if (on) ContextCompat.startForegroundService(ctx, intent) else ctx.stopService(intent)
    }

    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("BT Popup", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Shows a rotating-device card when your headphones connect.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            StepRow(
                "1  Bluetooth permission",
                if (btOk) "Granted" else "Needed to see which device connected",
                done = btOk, action = "Grant",
            ) {
                val perms = buildList {
                    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (perms.isNotEmpty()) permLauncher.launch(perms.toTypedArray())
            }
        }
        item {
            StepRow(
                "2  Display over other apps",
                if (overlayOk) "Granted" else "Needed to show the popup on top of any app",
                done = overlayOk, action = "Open settings",
            ) {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("3  Watch for connections", fontWeight = FontWeight.SemiBold)
                    Text("Runs a small background service", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, enabled = btOk && overlayOk, onCheckedChange = { setMonitoring(it) })
            }
        }
        item {
            Button(
                onClick = { PopupController.show(ctx, PopupInfo("Nothing Headphone (1)", DeviceKind.NOTHING_HEADPHONE, 80)) },
                enabled = overlayOk, modifier = Modifier.fillMaxWidth(),
            ) { Text("Preview popup (Nothing Headphone)") }
        }
        item { Spacer(Modifier.height(4.dp)); Text("Paired audio devices", fontWeight = FontWeight.SemiBold) }
        if (paired.isEmpty()) {
            item { Text(if (btOk) "No paired audio devices found." else "Grant Bluetooth permission to list them.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(paired, key = { it.address }) { d ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.name)
                    Text(d.kind.name.lowercase().replace('_', ' '), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                OutlinedButton(onClick = { PopupController.show(ctx, PopupInfo(d.name, d.kind, null)) }, enabled = overlayOk) { Text("Preview") }
            }
        }
    }
}

@Composable
private fun StepRow(title: String, subtitle: String, done: Boolean, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (done) Text("✓", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
        else Button(onClick = onClick) { Text(action) }
    }
}
