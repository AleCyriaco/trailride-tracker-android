package com.trailride.tracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trailride.tracker.data.db.entity.TrackPointEntity
import com.trailride.tracker.ui.theme.MetricElevation

@Composable
fun ElevationChart(trackPoints: List<TrackPointEntity>) {
    val altitudes = trackPoints.mapNotNull { it.altitude }
    if (altitudes.size < 2) return

    val minAlt = altitudes.min()
    val maxAlt = altitudes.max()
    val range = (maxAlt - minAlt).coerceAtLeast(1.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Elevation Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${String.format("%.0f", minAlt)} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${String.format("%.0f", maxAlt)} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val fillColor = MetricElevation.copy(alpha = 0.2f)
            val strokeColor = MetricElevation

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val w = size.width
                val h = size.height
                val step = w / (altitudes.size - 1).coerceAtLeast(1)

                val linePath = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, h)

                altitudes.forEachIndexed { i, alt ->
                    val x = i * step
                    val y = h - ((alt - minAlt) / range * h).toFloat()
                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(w, h)
                fillPath.close()

                drawPath(fillPath, fillColor)
                drawPath(linePath, strokeColor, style = Stroke(width = 2f))
            }
        }
    }
}
