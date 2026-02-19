package com.trailride.tracker.ui.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailride.tracker.data.db.entity.RideEntity
import com.trailride.tracker.ui.theme.CompareRideA
import com.trailride.tracker.ui.theme.CompareRideB
import com.trailride.tracker.util.Formatters
import com.trailride.tracker.viewmodel.CompareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: CompareViewModel = hiltViewModel(),
) {
    val rides by viewModel.rides.collectAsState()
    val rideA by viewModel.rideA.collectAsState()
    val rideB by viewModel.rideB.collectAsState()
    val selectedA by viewModel.selectedRideA.collectAsState()
    val selectedB by viewModel.selectedRideB.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "Compare Rides",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        // Ride A dropdown
        RideDropdown(
            label = "Ride A",
            rides = rides,
            selected = selectedA,
            onSelect = { viewModel.selectRideA(it) },
            accentColor = CompareRideA,
        )

        Spacer(Modifier.height(8.dp))

        // Ride B dropdown
        RideDropdown(
            label = "Ride B",
            rides = rides,
            selected = selectedB,
            onSelect = { viewModel.selectRideB(it) },
            accentColor = CompareRideB,
        )

        Spacer(Modifier.height(16.dp))

        // Chart
        if (rideA != null && rideB != null) {
            CompareChart(rideA = rideA!!, rideB = rideB!!)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Select two rides to compare",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideDropdown(
    label: String,
    rides: List<RideEntity>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    accentColor: Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedRide = rides.find { it.id == selected }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedRide?.trailName ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = accentColor) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            rides.forEach { ride ->
                DropdownMenuItem(
                    text = { Text("${ride.trailName} — ${Formatters.formatDateTime(ride.startTs)}") },
                    onClick = {
                        onSelect(ride.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CompareChart(rideA: RideEntity, rideB: RideEntity) {
    data class Metric(val label: String, val valueA: Double, val valueB: Double, val format: (Double) -> String)

    val metrics = listOf(
        Metric("Distance (km)", rideA.distanceMeters / 1000, rideB.distanceMeters / 1000) { String.format("%.2f", it) },
        Metric("Avg Speed (km/h)", rideA.avgSpeedMps * 3.6, rideB.avgSpeedMps * 3.6) { String.format("%.1f", it) },
        Metric("Max Speed (km/h)", rideA.maxSpeedMps * 3.6, rideB.maxSpeedMps * 3.6) { String.format("%.1f", it) },
        Metric("Total Time (min)", rideA.totalTimeSec / 60.0, rideB.totalTimeSec / 60.0) { String.format("%.0f", it) },
        Metric("Moving Time (min)", rideA.movingTimeSec / 60.0, rideB.movingTimeSec / 60.0) { String.format("%.0f", it) },
        Metric("Stopped (min)", rideA.stoppedTimeSec / 60.0, rideB.stoppedTimeSec / 60.0) { String.format("%.0f", it) },
        Metric("Elev Gain (m)", rideA.elevationGainMeters, rideB.elevationGainMeters) { String.format("%.0f", it) },
        Metric("Elev Loss (m)", rideA.elevationLossMeters, rideB.elevationLossMeters) { String.format("%.0f", it) },
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(12.dp)) { drawCircle(CompareRideA) }
                Spacer(Modifier.width(4.dp))
                Text("Ride A", style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(12.dp)) { drawCircle(CompareRideB) }
                Spacer(Modifier.width(4.dp))
                Text("Ride B", style = MaterialTheme.typography.labelMedium)
            }
        }

        metrics.forEach { metric ->
            val maxVal = maxOf(metric.valueA, metric.valueB).coerceAtLeast(0.001)

            Column {
                Text(
                    metric.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))

                // Bar A
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp),
                    ) {
                        val fraction = (metric.valueA / maxVal).toFloat().coerceIn(0f, 1f)
                        drawRoundRect(
                            color = CompareRideA,
                            size = Size(size.width * fraction, size.height),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(metric.format(metric.valueA), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(48.dp))
                }

                Spacer(Modifier.height(2.dp))

                // Bar B
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp),
                    ) {
                        val fraction = (metric.valueB / maxVal).toFloat().coerceIn(0f, 1f)
                        drawRoundRect(
                            color = CompareRideB,
                            size = Size(size.width * fraction, size.height),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(metric.format(metric.valueB), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(48.dp))
                }
            }
        }
    }
}
