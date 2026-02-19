package com.trailride.tracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trailride.tracker.data.db.entity.HealthSampleEntity
import com.trailride.tracker.ui.theme.MetricHeartRate

@Composable
fun HeartRateChart(samples: List<HealthSampleEntity>) {
    val hrSamples = samples.filter { it.sampleType == "hr" }
    if (hrSamples.size < 2) return

    val values = hrSamples.map { it.value }
    val minHr = values.min()
    val maxHr = values.max()
    val range = (maxHr - minHr).coerceAtLeast(1.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Heart Rate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${minHr.toInt()} bpm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${maxHr.toInt()} bpm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val fillColor = MetricHeartRate.copy(alpha = 0.2f)
            val strokeColor = MetricHeartRate

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val w = size.width
                val h = size.height
                val step = w / (values.size - 1).coerceAtLeast(1)

                val linePath = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, h)

                values.forEachIndexed { i, hr ->
                    val x = i * step
                    val y = h - ((hr - minHr) / range * h).toFloat()
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
