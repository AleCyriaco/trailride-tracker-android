package com.trailride.tracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailride.tracker.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val storeHealth by viewModel.storeHealthSamples.collectAsState()
    val maxHr by viewModel.maxHr.collectAsState()
    val minAccuracy by viewModel.minAccuracy.collectAsState()
    val autoStopDelay by viewModel.autoStopDelay.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))

        // Health section
        Text(
            "Health",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Store Health Samples", modifier = Modifier.weight(1f))
            Switch(
                checked = storeHealth,
                onCheckedChange = { viewModel.setStoreHealthSamples(it) },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Max HR Assumption", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = maxHr.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> viewModel.setMaxHr(v) } },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("bpm") },
            )
        }

        Spacer(Modifier.height(24.dp))

        // GPS section
        Text(
            "GPS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Min Accuracy", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = minAccuracy.toInt().toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.setMinAccuracy(v) } },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("m") },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto-Stop Delay", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = autoStopDelay.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> viewModel.setAutoStopDelay(v) } },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("s") },
            )
        }

        Spacer(Modifier.weight(1f))

        // About
        Text(
            "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Version", modifier = Modifier.weight(1f))
            Text(
                "1.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
