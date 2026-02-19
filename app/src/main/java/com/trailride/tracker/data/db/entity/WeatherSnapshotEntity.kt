package com.trailride.tracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weatherSnapshots",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["rideId"])],
)
data class WeatherSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    val ts: Long,
    val temperatureC: Double? = null,
    val windspeedKmh: Double? = null,
    val weatherCode: Int = 0,
    val source: String = "open-meteo",
)
