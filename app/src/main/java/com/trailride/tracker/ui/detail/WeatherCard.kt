package com.trailride.tracker.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trailride.tracker.data.db.entity.WeatherSnapshotEntity
import com.trailride.tracker.util.WeatherCodeMapper

@Composable
fun WeatherCard(snapshots: List<WeatherSnapshotEntity>) {
    val latest = snapshots.lastOrNull() ?: return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Weather",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = WeatherCodeMapper.icon(latest.weatherCode),
                    contentDescription = WeatherCodeMapper.description(latest.weatherCode),
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        WeatherCodeMapper.description(latest.weatherCode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        latest.temperatureC?.let {
                            Text(
                                "${String.format("%.1f", it)} °C",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        latest.windspeedKmh?.let {
                            Text(
                                "${String.format("%.0f", it)} km/h wind",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
