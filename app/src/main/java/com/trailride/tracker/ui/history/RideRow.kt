package com.trailride.tracker.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trailride.tracker.data.db.entity.RideEntity
import com.trailride.tracker.ui.theme.MetricDistance
import com.trailride.tracker.ui.theme.MetricTime
import com.trailride.tracker.util.Formatters

@Composable
fun RideRow(
    ride: RideEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ride.trailName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = Formatters.formatDistance(ride.distanceMeters),
                        color = MetricDistance,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = Formatters.formatDuration(ride.totalTimeSec),
                        color = MetricTime,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.formatDateTime(ride.startTs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
