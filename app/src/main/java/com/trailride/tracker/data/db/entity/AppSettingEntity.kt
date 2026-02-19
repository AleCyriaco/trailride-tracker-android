package com.trailride.tracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appSettings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)
