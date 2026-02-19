package com.trailride.tracker.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailride.tracker.data.db.entity.RideEntity
import com.trailride.tracker.data.db.entity.StopEntity
import com.trailride.tracker.data.db.entity.WeatherSnapshotEntity
import com.trailride.tracker.data.repository.RideRepository
import com.trailride.tracker.service.RideMetrics
import com.trailride.tracker.service.TrackingService
import com.trailride.tracker.service.TrackingState
import com.trailride.tracker.service.WeatherService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LiveRideViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: RideRepository,
    private val weatherService: WeatherService,
) : ViewModel() {

    private var trackingService: TrackingService? = null
    private var bound = false

    private val _state = MutableStateFlow(TrackingState.IDLE)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _metrics = MutableStateFlow(RideMetrics())
    val metrics: StateFlow<RideMetrics> = _metrics.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<TrackingService.LatLng>>(emptyList())
    val trackPoints: StateFlow<List<TrackingService.LatLng>> = _trackPoints.asStateFlow()

    private val _trailName = MutableStateFlow("")
    val trailName: StateFlow<String> = _trailName.asStateFlow()

    private var currentRide: RideEntity? = null
    private var pendingStopId: Long? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as TrackingService.LocalBinder
            trackingService = binder.service
            bound = true

            // Collect service state flows
            viewModelScope.launch {
                binder.service.state.collect { _state.value = it }
            }
            viewModelScope.launch {
                binder.service.metrics.collect { _metrics.value = it }
            }
            viewModelScope.launch {
                binder.service.trackPoints.collect { _trackPoints.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            trackingService = null
            bound = false
        }
    }

    init {
        bindService()
        generateDefaultTrailName()
    }

    private fun bindService() {
        val intent = Intent(appContext, TrackingService::class.java)
        appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun updateTrailName(name: String) {
        _trailName.value = name
    }

    fun startRide() {
        viewModelScope.launch {
            val name = _trailName.value.ifBlank { generateDefaultTrailName() }
            val ride = repository.createRide(name)
            currentRide = ride

            val service = trackingService ?: return@launch

            service.onNewTrackPoint = { point ->
                viewModelScope.launch {
                    repository.insertTrackPoint(point)
                }
            }
            service.onStopDetected = { stop ->
                viewModelScope.launch {
                    val id = repository.insertStop(stop)
                    pendingStopId = id
                }
            }
            service.onStopEnded = { stop ->
                viewModelScope.launch {
                    pendingStopId?.let { id ->
                        repository.updateStop(stop.copy(id = id))
                        pendingStopId = null
                    }
                }
            }

            // Start foreground service
            val intent = Intent(appContext, TrackingService::class.java)
            appContext.startForegroundService(intent)
            service.startTracking(ride.id)

            // Fetch weather
            fetchWeather(ride.id)
        }
    }

    fun pauseRide() {
        trackingService?.pauseTracking()
    }

    fun resumeRide() {
        trackingService?.resumeTracking()
    }

    fun stopRide() {
        viewModelScope.launch {
            trackingService?.stopTracking()
            val ride = currentRide ?: return@launch
            val m = _metrics.value
            val updated = ride.copy(
                endTs = System.currentTimeMillis(),
                status = "completed",
                distanceMeters = m.distanceMeters,
                totalTimeSec = m.totalTimeSec,
                movingTimeSec = m.movingTimeSec,
                stoppedTimeSec = m.stoppedTimeSec,
                avgSpeedMps = m.avgSpeedMps,
                maxSpeedMps = m.maxSpeedMps,
                elevationGainMeters = m.elevationGainMeters,
                elevationLossMeters = m.elevationLossMeters,
            )
            repository.updateRide(updated)
            currentRide = null
            generateDefaultTrailName()
        }
    }

    private fun fetchWeather(rideId: Long) {
        viewModelScope.launch {
            val m = _metrics.value
            if (m.currentLat == 0.0 && m.currentLon == 0.0) return@launch
            val reading = weatherService.fetchCurrent(m.currentLat, m.currentLon)
            val snapshot = WeatherSnapshotEntity(
                rideId = rideId,
                ts = System.currentTimeMillis(),
                temperatureC = reading.temperatureC,
                windspeedKmh = reading.windspeedKmh,
                weatherCode = reading.weatherCode,
                source = reading.source,
            )
            repository.insertWeatherSnapshot(snapshot)
        }
    }

    private fun generateDefaultTrailName(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val name = "Ride ${format.format(Date())}"
        _trailName.value = name
        return name
    }

    override fun onCleared() {
        super.onCleared()
        if (bound) {
            appContext.unbindService(connection)
            bound = false
        }
    }
}
