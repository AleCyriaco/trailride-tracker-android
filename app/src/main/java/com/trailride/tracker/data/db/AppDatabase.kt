package com.trailride.tracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trailride.tracker.data.db.dao.RideDao
import com.trailride.tracker.data.db.entity.*

@Database(
    entities = [
        RideEntity::class,
        TrackPointEntity::class,
        StopEntity::class,
        WeatherSnapshotEntity::class,
        SensorDeviceEntity::class,
        SensorSampleEntity::class,
        HealthSummaryEntity::class,
        HealthSampleEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
}
