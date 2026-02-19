package com.trailride.tracker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.trailride.tracker.MainActivity
import com.trailride.tracker.R
import com.trailride.tracker.TrailRideApp
import com.trailride.tracker.data.db.entity.StopEntity
import com.trailride.tracker.data.db.entity.TrackPointEntity
import com.trailride.tracker.util.GeoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RideMetrics(
    val distanceMeters: Double = 0.0,
    val movingTimeSec: Int = 0,
    val stoppedTimeSec: Int = 0,
    val avgSpeedMps: Double = 0.0,
    val maxSpeedMps: Double = 0.0,
    val currentSpeedMps: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val currentAltitude: Double = 0.0,
    val currentLat: Double = 0.0,
    val currentLon: Double = 0.0,
) {
    val totalTimeSec: Int get() = movingTimeSec + stoppedTimeSec
}

enum class TrackingState { IDLE, TRACKING, PAUSED }

class TrackingService : Service() {

    // Constants matching iOS
    companion object {
        private const val MAX_ACCEPTED_ACCURACY = 30.0   // meters
        private const val MAX_SEGMENT_JUMP = 80.0        // meters in <2s
        private const val ELEVATION_NOISE_THRESHOLD = 2.0 // meters
        private const val MOVING_SPEED_THRESHOLD = 1.0   // m/s (3.6 km/h)
        private const val AUTO_STOP_DELAY = 15            // seconds
    }

    // Binder
    inner class LocalBinder : Binder() {
        val service: TrackingService get() = this@TrackingService
    }

    private val binder = LocalBinder()

    // State
    private val _state = MutableStateFlow(TrackingState.IDLE)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _metrics = MutableStateFlow(RideMetrics())
    val metrics: StateFlow<RideMetrics> = _metrics.asStateFlow()

    data class LatLng(val lat: Double, val lon: Double)
    private val _trackPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val trackPoints: StateFlow<List<LatLng>> = _trackPoints.asStateFlow()

    // Callbacks
    var onNewTrackPoint: ((TrackPointEntity) -> Unit)? = null
    var onStopDetected: ((StopEntity) -> Unit)? = null
    var onStopEnded: ((StopEntity) -> Unit)? = null

    // Private
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var rideId: Long = 0

    private var lastAcceptedLat: Double? = null
    private var lastAcceptedLon: Double? = null
    private var lastAcceptedAlt: Double? = null
    private var lastAcceptedTimeMs: Long? = null

    // Weighted average speed
    private var weightedSpeedSum: Double = 0.0
    private var weightedSpeedTime: Int = 0

    // Auto-stop detection
    private var slowSinceMs: Long? = null
    private var currentStop: StopEntity? = null
    private var currentStopId: Long? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startTracking(rideId: Long) {
        this.rideId = rideId
        resetAccumulator()
        _state.value = TrackingState.TRACKING
        startForeground(TrailRideApp.TRACKING_NOTIFICATION_ID, buildNotification())
        requestLocationUpdates()
    }

    fun pauseTracking() {
        _state.value = TrackingState.PAUSED
        removeLocationUpdates()
    }

    fun resumeTracking() {
        _state.value = TrackingState.TRACKING
        requestLocationUpdates()
    }

    fun stopTracking() {
        _state.value = TrackingState.IDLE
        removeLocationUpdates()
        // Close any open stop
        currentStop?.let { stop ->
            if (stop.endTs == null) {
                val closedStop = stop.copy(endTs = System.currentTimeMillis())
                onStopEnded?.invoke(closedStop)
                currentStop = null
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(3f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (_state.value != TrackingState.TRACKING) return
                for (location in result.locations) {
                    processLocation(
                        lat = location.latitude,
                        lon = location.longitude,
                        altitude = location.altitude,
                        accuracy = location.accuracy.toDouble(),
                        timeMs = location.time,
                    )
                }
            }
        }
        locationCallback = callback
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun removeLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun processLocation(
        lat: Double,
        lon: Double,
        altitude: Double,
        accuracy: Double,
        timeMs: Long,
    ) {
        // Filter 1: accuracy
        if (accuracy > MAX_ACCEPTED_ACCURACY || accuracy < 0) return

        val prevLat = lastAcceptedLat
        val prevLon = lastAcceptedLon
        val prevTimeMs = lastAcceptedTimeMs

        if (prevLat != null && prevLon != null && prevTimeMs != null) {
            val dtSec = (timeMs - prevTimeMs) / 1000.0
            if (dtSec <= 0) return

            val segmentDistance = GeoUtils.haversineMeters(prevLat, prevLon, lat, lon)

            // Filter 2: impossible jump
            if (segmentDistance > MAX_SEGMENT_JUMP && dtSec <= 2) return

            // Filter 3: hard velocity check (>72 km/h = 20 m/s)
            if (dtSec > 0 && segmentDistance / dtSec > 20) return

            val speed = segmentDistance / dtSec
            val dtInt = dtSec.toInt()
            val current = _metrics.value

            var newMetrics = current.copy(
                currentSpeedMps = speed,
                currentLat = lat,
                currentLon = lon,
                currentAltitude = altitude,
                distanceMeters = current.distanceMeters + segmentDistance,
            )

            if (speed >= MOVING_SPEED_THRESHOLD) {
                newMetrics = newMetrics.copy(
                    movingTimeSec = current.movingTimeSec + dtInt,
                    maxSpeedMps = maxOf(current.maxSpeedMps, speed),
                )
                weightedSpeedSum += speed * dtSec
                weightedSpeedTime += dtInt
                handleMoving(timeMs)
            } else {
                newMetrics = newMetrics.copy(
                    stoppedTimeSec = current.stoppedTimeSec + dtInt,
                )
                handleSlow(lat, lon, timeMs)
            }

            // Weighted average speed
            if (weightedSpeedTime > 0) {
                newMetrics = newMetrics.copy(
                    avgSpeedMps = weightedSpeedSum / weightedSpeedTime,
                )
            }

            // Elevation
            lastAcceptedAlt?.let { prevAlt ->
                val elevDelta = altitude - prevAlt
                if (kotlin.math.abs(elevDelta) >= ELEVATION_NOISE_THRESHOLD) {
                    newMetrics = if (elevDelta > 0) {
                        newMetrics.copy(elevationGainMeters = current.elevationGainMeters + elevDelta)
                    } else {
                        newMetrics.copy(elevationLossMeters = current.elevationLossMeters + kotlin.math.abs(elevDelta))
                    }
                }
            }

            _metrics.value = newMetrics

            // Emit track point
            val point = TrackPointEntity(
                rideId = rideId,
                ts = timeMs,
                lat = lat,
                lon = lon,
                altitude = altitude,
                accuracy = accuracy,
                speed = speed,
                segmentDistance = segmentDistance,
            )
            onNewTrackPoint?.invoke(point)
            _trackPoints.value = _trackPoints.value + LatLng(lat, lon)

        } else {
            // First point
            _metrics.value = _metrics.value.copy(
                currentLat = lat,
                currentLon = lon,
                currentAltitude = altitude,
            )
            val point = TrackPointEntity(
                rideId = rideId,
                ts = timeMs,
                lat = lat,
                lon = lon,
                altitude = altitude,
                accuracy = accuracy,
                speed = 0.0,
                segmentDistance = 0.0,
            )
            onNewTrackPoint?.invoke(point)
            _trackPoints.value = _trackPoints.value + LatLng(lat, lon)
        }

        lastAcceptedLat = lat
        lastAcceptedLon = lon
        lastAcceptedAlt = altitude
        lastAcceptedTimeMs = timeMs
    }

    // Auto-stop detection
    private fun handleSlow(lat: Double, lon: Double, timeMs: Long) {
        if (slowSinceMs == null) {
            slowSinceMs = timeMs
        }
        val slowSince = slowSinceMs ?: return
        if ((timeMs - slowSince) >= AUTO_STOP_DELAY * 1000L && currentStop == null) {
            val stop = StopEntity(
                rideId = rideId,
                startTs = slowSince,
                endTs = null,
                lat = lat,
                lon = lon,
            )
            currentStop = stop
            onStopDetected?.invoke(stop)
        }
    }

    private fun handleMoving(timeMs: Long) {
        slowSinceMs = null
        currentStop?.let { stop ->
            val closed = stop.copy(endTs = timeMs)
            onStopEnded?.invoke(closed)
            currentStop = null
        }
    }

    private fun resetAccumulator() {
        _metrics.value = RideMetrics()
        _trackPoints.value = emptyList()
        lastAcceptedLat = null
        lastAcceptedLon = null
        lastAcceptedAlt = null
        lastAcceptedTimeMs = null
        weightedSpeedSum = 0.0
        weightedSpeedTime = 0
        slowSinceMs = null
        currentStop = null
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, TrailRideApp.TRACKING_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText("Tracking your ride...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
