package com.trailride.tracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensorDevices",
    indices = [Index(value = ["deviceId"], unique = true)],
)
data class SensorDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val name: String? = null,
    val sensorType: String, // hr, cadence, power
    val lastSeenTs: Long,
)
