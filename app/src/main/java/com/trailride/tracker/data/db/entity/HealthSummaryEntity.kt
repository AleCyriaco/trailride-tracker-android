package com.trailride.tracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "healthSummaries",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["rideId"], unique = true)],
)
data class HealthSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    val dataSource: String,
    val avgHr: Double? = null,
    val maxHr: Double? = null,
    val calories: Double? = null,
    val zoneZ1Sec: Int = 0,
    val zoneZ2Sec: Int = 0,
    val zoneZ3Sec: Int = 0,
    val zoneZ4Sec: Int = 0,
    val zoneZ5Sec: Int = 0,
)
