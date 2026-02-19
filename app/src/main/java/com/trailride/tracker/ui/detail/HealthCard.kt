package com.trailride.tracker.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trailride.tracker.data.db.entity.HealthSummaryEntity
import com.trailride.tracker.ui.theme.*
import com.trailride.tracker.util.Formatters

@Composable
fun HealthCard(summary: HealthSummaryEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avg HR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${summary.avgHr?.toInt() ?: "-"} bpm",
                        color = MetricHeartRate,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Max HR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${summary.maxHr?.toInt() ?: "-"} bpm",
                        color = MetricHeartRate,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (summary.calories != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calories", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${summary.calories!!.toInt()} kcal",
                            color = MetricCalories,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "HR Zones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            val zones = listOf(
                "Z1 (<60%)" to summary.zoneZ1Sec,
                "Z2 (60-70%)" to summary.zoneZ2Sec,
                "Z3 (70-80%)" to summary.zoneZ3Sec,
                "Z4 (80-90%)" to summary.zoneZ4Sec,
                "Z5 (>90%)" to summary.zoneZ5Sec,
            )

            val totalZone = zones.sumOf { it.second }.coerceAtLeast(1)

            zones.forEach { (label, seconds) ->
                val fraction = seconds.toFloat() / totalZone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(80.dp),
                    )
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = MetricHeartRate,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Formatters.formatDuration(seconds),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }

            Text(
                "Source: ${summary.dataSource}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
