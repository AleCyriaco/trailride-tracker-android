package com.trailride.tracker.data.repository

import com.trailride.tracker.data.db.dao.RideDao
import com.trailride.tracker.data.db.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val dao: RideDao,
) {
    // -- Rides --

    suspend fun createRide(trailName: String): RideEntity {
        val ride = RideEntity(
            trailName = trailName,
            startTs = System.currentTimeMillis(),
        )
        val id = dao.insertRide(ride)
        return ride.copy(id = id)
    }

    suspend fun updateRide(ride: RideEntity) = dao.updateRide(ride)

    suspend fun deleteRide(id: Long) = dao.deleteRide(id)

    suspend fun getRide(id: Long): RideEntity? = dao.getRide(id)

    fun getAllRidesFlow(): Flow<List<RideEntity>> = dao.getAllRidesFlow()

    suspend fun getAllRides(): List<RideEntity> = dao.getAllRides()

    suspend fun getCompletedRides(): List<RideEntity> = dao.getCompletedRides()

    fun getCompletedRidesFlow(): Flow<List<RideEntity>> = dao.getCompletedRidesFlow()

    suspend fun searchRides(query: String): List<RideEntity> = dao.searchRides(query)

    suspend fun getTrailNames(): List<String> = dao.getTrailNames()

    // -- Track Points --

    suspend fun insertTrackPoint(point: TrackPointEntity) = dao.insertTrackPoint(point)

    suspend fun getTrackPoints(rideId: Long): List<TrackPointEntity> = dao.getTrackPoints(rideId)

    // -- Stops --

    suspend fun insertStop(stop: StopEntity): Long = dao.insertStop(stop)

    suspend fun updateStop(stop: StopEntity) = dao.updateStop(stop)

    suspend fun getStops(rideId: Long): List<StopEntity> = dao.getStops(rideId)

    // -- Weather --

    suspend fun insertWeatherSnapshot(snapshot: WeatherSnapshotEntity) =
        dao.insertWeatherSnapshot(snapshot)

    suspend fun getWeatherSnapshots(rideId: Long): List<WeatherSnapshotEntity> =
        dao.getWeatherSnapshots(rideId)

    // -- Sensor Devices --

    suspend fun upsertSensorDevice(device: SensorDeviceEntity) = dao.upsertSensorDevice(device)

    suspend fun getSensorDevices(): List<SensorDeviceEntity> = dao.getSensorDevices()

    // -- Sensor Samples --

    suspend fun insertSensorSample(sample: SensorSampleEntity) = dao.insertSensorSample(sample)

    suspend fun getSensorSamples(rideId: Long): List<SensorSampleEntity> =
        dao.getSensorSamples(rideId)

    suspend fun getSensorSamplesByType(rideId: Long, type: String): List<SensorSampleEntity> =
        dao.getSensorSamplesByType(rideId, type)

    // -- Health Summary --

    suspend fun upsertHealthSummary(summary: HealthSummaryEntity) =
        dao.upsertHealthSummary(summary)

    suspend fun getHealthSummary(rideId: Long): HealthSummaryEntity? =
        dao.getHealthSummary(rideId)

    // -- Health Samples --

    suspend fun insertHealthSamples(samples: List<HealthSampleEntity>) =
        dao.insertHealthSamples(samples)

    suspend fun getHealthSamples(rideId: Long): List<HealthSampleEntity> =
        dao.getHealthSamples(rideId)

    suspend fun getHealthSamplesByType(rideId: Long, type: String): List<HealthSampleEntity> =
        dao.getHealthSamplesByType(rideId, type)

    // -- App Settings --

    suspend fun getSetting(key: String): String? = dao.getSetting(key)

    suspend fun setSetting(key: String, value: String) =
        dao.setSetting(AppSettingEntity(key, value))

    // -- Ride Detail (aggregate) --

    suspend fun getRideDetail(id: Long): RideDetail? {
        val ride = dao.getRide(id) ?: return null
        return RideDetail(
            ride = ride,
            trackPoints = dao.getTrackPoints(id),
            stops = dao.getStops(id),
            weatherSnapshots = dao.getWeatherSnapshots(id),
            sensorSamples = dao.getSensorSamples(id),
            healthSummary = dao.getHealthSummary(id),
            healthSamples = dao.getHealthSamples(id),
        )
    }
}
