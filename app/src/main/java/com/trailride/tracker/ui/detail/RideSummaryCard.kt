package com.trailride.tracker.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trailride.tracker.data.db.entity.RideEntity
import com.trailride.tracker.ui.theme.*
import com.trailride.tracker.util.Formatters

@Composable
fun RideSummaryCard(ride: RideEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem("Distance", Formatters.formatDistance(ride.distanceMeters), MetricDistance, Modifier.weight(1f))
                SummaryItem("Avg Speed", Formatters.formatSpeedKmh(ride.avgSpeedMps), MetricSpeed, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem("Total Time", Formatters.formatDuration(ride.totalTimeSec), MetricTime, Modifier.weight(1f))
                SummaryItem("Moving Time", Formatters.formatDuration(ride.movingTimeSec), MetricTime, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem("Max Speed", Formatters.formatSpeedKmh(ride.maxSpeedMps), MetricSpeed, Modifier.weight(1f))
                SummaryItem("Stopped", Formatters.formatDuration(ride.stoppedTimeSec), MetricTime, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem("Elev Gain", Formatters.formatElevation(ride.elevationGainMeters), MetricElevation, Modifier.weight(1f))
                SummaryItem("Elev Loss", Formatters.formatElevation(ride.elevationLossMeters), MetricElevation, Modifier.weight(1f))
            }

            if (ride.notes?.isNotBlank() == true) {
                Spacer(Modifier.height(8.dp))
                Text(
                    ride.notes!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
