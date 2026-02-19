package com.trailride.tracker.service

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.trailride.tracker.data.db.entity.HealthSampleEntity
import com.trailride.tracker.data.db.entity.HealthSummaryEntity
import java.time.Instant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val MAX_HR_ASSUMPTION = 190.0

        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
        )
    }

    private val client: HealthConnectClient? by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun hasPermissions(): Boolean {
        val hc = client ?: return false
        return try {
            val granted = hc.permissionController.getGrantedPermissions()
            PERMISSIONS.all { it in granted }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun queryLiveHeartRate(startMs: Long, endMs: Long): Double? {
        val hc = client ?: return null
        return try {
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startMs),
                        Instant.ofEpochMilli(endMs),
                    ),
                ),
            )
            response.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute?.toDouble()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importForRide(
        rideId: Long,
        startMs: Long,
        endMs: Long,
    ): Pair<HealthSummaryEntity, List<HealthSampleEntity>>? {
        val hc = client ?: return null

        return try {
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startMs),
                        Instant.ofEpochMilli(endMs),
                    ),
                ),
            )

            val allSamples = response.records.flatMap { record ->
                record.samples.map { sample ->
                    sample.beatsPerMinute.toDouble() to sample.time.toEpochMilli()
                }
            }.sortedBy { it.second }

            if (allSamples.isEmpty()) return null

            val bpmValues = allSamples.map { it.first }
            val avgHr = bpmValues.average()
            val maxHr = bpmValues.max()

            // Zone calculation
            var z1 = 0; var z2 = 0; var z3 = 0; var z4 = 0; var z5 = 0
            for (i in bpmValues.indices) {
                val ratio = bpmValues[i] / MAX_HR_ASSUMPTION
                val duration = if (i + 1 < allSamples.size) {
                    maxOf(1, ((allSamples[i + 1].second - allSamples[i].second) / 1000).toInt())
                } else {
                    1
                }
                when {
                    ratio < 0.6 -> z1 += duration
                    ratio < 0.7 -> z2 += duration
                    ratio < 0.8 -> z3 += duration
                    ratio < 0.9 -> z4 += duration
                    else -> z5 += duration
                }
            }

            val summary = HealthSummaryEntity(
                rideId = rideId,
                dataSource = "Health Connect",
                avgHr = avgHr,
                maxHr = maxHr,
                calories = null, // Health Connect doesn't provide calories the same way
                zoneZ1Sec = z1,
                zoneZ2Sec = z2,
                zoneZ3Sec = z3,
                zoneZ4Sec = z4,
                zoneZ5Sec = z5,
            )

            // Downsample to ~10s intervals
            val healthSamples = mutableListOf<HealthSampleEntity>()
            var lastSampleTs = 0L
            for ((bpm, ts) in allSamples) {
                if (ts - lastSampleTs < 10_000) continue
                lastSampleTs = ts
                healthSamples.add(
                    HealthSampleEntity(
                        rideId = rideId,
                        ts = ts,
                        sampleType = "hr",
                        value = bpm,
                        unit = "bpm",
                        source = "Health Connect",
                    ),
                )
            }

            summary to healthSamples
        } catch (_: Exception) {
            null
        }
    }
}
