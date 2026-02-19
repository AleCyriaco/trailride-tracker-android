package com.trailride.tracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trailName: String,
    val startTs: Long,
    val endTs: Long? = null,
    val status: String = "in_progress", // in_progress, paused, completed
    val distanceMeters: Double = 0.0,
    val totalTimeSec: Int = 0,
    val movingTimeSec: Int = 0,
    val stoppedTimeSec: Int = 0,
    val avgSpeedMps: Double = 0.0,
    val maxSpeedMps: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val notes: String? = null,
)
