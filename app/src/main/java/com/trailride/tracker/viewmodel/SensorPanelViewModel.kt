package com.trailride.tracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailride.tracker.data.db.entity.SensorSampleEntity
import com.trailride.tracker.data.repository.RideRepository
import com.trailride.tracker.service.BleSensorService
import com.trailride.tracker.service.DiscoveredDevice
import com.trailride.tracker.service.SensorType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorPanelViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: RideRepository,
) : ViewModel() {

    private val bleService = BleSensorService(appContext)

    val discovered: StateFlow<List<DiscoveredDevice>> = bleService.discovered
    val connectedAddresses: StateFlow<Set<String>> = bleService.connectedAddresses
    val latestReadings: StateFlow<Map<SensorType, Double>> = bleService.latestReadings

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var currentRideId: Long? = null

    fun setRideId(rideId: Long) {
        currentRideId = rideId
        bleService.onSensorReading = { type, value ->
            viewModelScope.launch {
                val rid = currentRideId ?: return@launch
                val sample = SensorSampleEntity(
                    rideId = rid,
                    ts = System.currentTimeMillis(),
                    sensorType = type.value,
                    value = value,
                    unit = when (type) {
                        SensorType.HR -> "bpm"
                        SensorType.CADENCE -> "rpm"
                        SensorType.POWER -> "W"
                    },
                    deviceId = "", // populated by device
                )
                repository.insertSensorSample(sample)
            }
        }
    }

    fun startScan() {
        _isScanning.value = true
        bleService.startScan()
        viewModelScope.launch {
            kotlinx.coroutines.delay(8500)
            _isScanning.value = false
        }
    }

    fun connect(device: DiscoveredDevice) {
        bleService.connect(device.device)
    }

    fun disconnectAll() {
        bleService.disconnectAll()
    }

    override fun onCleared() {
        super.onCleared()
        bleService.disconnectAll()
    }
}
