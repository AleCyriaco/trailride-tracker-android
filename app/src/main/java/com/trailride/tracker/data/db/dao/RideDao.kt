package com.trailride.tracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trailride.tracker.data.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    // -- Rides --

    @Insert
    suspend fun insertRide(ride: RideEntity): Long

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteRide(id: Long)

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getRide(id: Long): RideEntity?

    @Query("SELECT * FROM rides ORDER BY startTs DESC")
    fun getAllRidesFlow(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides ORDER BY startTs DESC")
    suspend fun getAllRides(): List<RideEntity>

    @Query("SELECT * FROM rides WHERE status = 'completed' ORDER BY startTs DESC")
    suspend fun getCompletedRides(): List<RideEntity>

    @Query("SELECT * FROM rides WHERE status = 'completed' ORDER BY startTs DESC")
    fun getCompletedRidesFlow(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE trailName LIKE '%' || :query || '%' ORDER BY startTs DESC")
    suspend fun searchRides(query: String): List<RideEntity>

    @Query("SELECT DISTINCT trailName FROM rides ORDER BY trailName")
    suspend fun getTrailNames(): List<String>

    // -- Track Points --

    @Insert
    suspend fun insertTrackPoint(point: TrackPointEntity): Long

    @Insert
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM trackPoints WHERE rideId = :rideId ORDER BY ts ASC")
    suspend fun getTrackPoints(rideId: Long): List<TrackPointEntity>

    // -- Stops --

    @Insert
    suspend fun insertStop(stop: StopEntity): Long

    @Update
    suspend fun updateStop(stop: StopEntity)

    @Query("SELECT * FROM stops WHERE rideId = :rideId ORDER BY startTs ASC")
    suspend fun getStops(rideId: Long): List<StopEntity>

    // -- Weather --

    @Insert
    suspend fun insertWeatherSnapshot(snapshot: WeatherSnapshotEntity): Long

    @Query("SELECT * FROM weatherSnapshots WHERE rideId = :rideId ORDER BY ts ASC")
    suspend fun getWeatherSnapshots(rideId: Long): List<WeatherSnapshotEntity>

    // -- Sensor Devices --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSensorDevice(device: SensorDeviceEntity)

    @Query("SELECT * FROM sensorDevices")
    suspend fun getSensorDevices(): List<SensorDeviceEntity>

    // -- Sensor Samples --

    @Insert
    suspend fun insertSensorSample(sample: SensorSampleEntity): Long

    @Query("SELECT * FROM sensorSamples WHERE rideId = :rideId ORDER BY ts ASC")
    suspend fun getSensorSamples(rideId: Long): List<SensorSampleEntity>

    @Query("SELECT * FROM sensorSamples WHERE rideId = :rideId AND sensorType = :type ORDER BY ts ASC")
    suspend fun getSensorSamplesByType(rideId: Long, type: String): List<SensorSampleEntity>

    // -- Health Summary --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthSummary(summary: HealthSummaryEntity)

    @Query("SELECT * FROM healthSummaries WHERE rideId = :rideId")
    suspend fun getHealthSummary(rideId: Long): HealthSummaryEntity?

    // -- Health Samples --

    @Insert
    suspend fun insertHealthSamples(samples: List<HealthSampleEntity>)

    @Query("SELECT * FROM healthSamples WHERE rideId = :rideId ORDER BY ts ASC")
    suspend fun getHealthSamples(rideId: Long): List<HealthSampleEntity>

    @Query("SELECT * FROM healthSamples WHERE rideId = :rideId AND sampleType = :type ORDER BY ts ASC")
    suspend fun getHealthSamplesByType(rideId: Long, type: String): List<HealthSampleEntity>

    // -- App Settings --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)

    @Query("SELECT value FROM appSettings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?
}
