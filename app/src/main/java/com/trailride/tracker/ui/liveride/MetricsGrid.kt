package com.trailride.tracker.ui.liveride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trailride.tracker.service.RideMetrics
import com.trailride.tracker.ui.theme.*
import com.trailride.tracker.util.Formatters

@Composable
fun MetricsGrid(metrics: RideMetrics) {
    val items = listOf(
        MetricItem("Distance", Formatters.formatDistance(metrics.distanceMeters), MetricDistance),
        MetricItem("Avg Speed", Formatters.formatSpeedKmh(metrics.avgSpeedMps), MetricSpeed),
        MetricItem("Time", Formatters.formatDuration(metrics.totalTimeSec), MetricTime),
        MetricItem("Elevation", Formatters.formatElevation(metrics.elevationGainMeters), MetricElevation),
        MetricItem("Max Speed", Formatters.formatSpeedKmh(metrics.maxSpeedMps), MetricSpeed),
        MetricItem("Current", Formatters.formatSpeedKmh(metrics.currentSpeedMps), MetricSpeed),
        MetricItem("Moving", Formatters.formatDuration(metrics.movingTimeSec), MetricTime),
        MetricItem("Elev Loss", Formatters.formatElevation(metrics.elevationLossMeters), MetricElevation),
    )

    // Use Row pairs instead of LazyVerticalGrid to work inside scrollable Column
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    MetricCard(item, Modifier.weight(1f))
                }
                // Fill remaining space if odd number
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class MetricItem(val label: String, val value: String, val color: Color)

@Composable
private fun MetricCard(item: MetricItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = item.color,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
