package com.trailride.tracker.ui.liveride

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.trailride.tracker.service.SensorType
import com.trailride.tracker.ui.theme.*
import com.trailride.tracker.viewmodel.SensorPanelViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BleSensorPanel(
    viewModel: SensorPanelViewModel = hiltViewModel(),
) {
    val discovered by viewModel.discovered.collectAsState()
    val connected by viewModel.connectedAddresses.collectAsState()
    val readings by viewModel.latestReadings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val blePermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        ),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sensors", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {
                        if (blePermissions.allPermissionsGranted) {
                            viewModel.startScan()
                        } else {
                            blePermissions.launchMultiplePermissionRequest()
                        }
                    },
                    enabled = !isScanning,
                ) {
                    Text(if (isScanning) "Scanning..." else "Scan")
                }
            }

            // Show readings
            if (readings.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                readings.forEach { (type, value) ->
                    val (label, color, unit) = when (type) {
                        SensorType.HR -> Triple("Heart Rate", MetricHeartRate, "bpm")
                        SensorType.CADENCE -> Triple("Cadence", MetricCadence, "rpm")
                        SensorType.POWER -> Triple("Power", MetricPower, "W")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = color, modifier = Modifier.weight(1f))
                        Text("${value.toInt()} $unit", color = color)
                    }
                }
            }

            // Show discovered devices
            if (discovered.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                discovered.forEach { device ->
                    val isConnected = connected.contains(device.address)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            device.name ?: device.address,
                            modifier = Modifier.weight(1f),
                        )
                        if (!isConnected) {
                            TextButton(onClick = { viewModel.connect(device) }) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}
