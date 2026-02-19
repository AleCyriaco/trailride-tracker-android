package com.trailride.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailride.tracker.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveHealthViewModel @Inject constructor(
    private val healthConnectService: HealthConnectService,
) : ViewModel() {

    private val _currentHr = MutableStateFlow<Double?>(null)
    val currentHr: StateFlow<Double?> = _currentHr.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var polling = false
    private var rideStartMs: Long = 0

    fun checkPermissions() {
        viewModelScope.launch {
            _hasPermission.value = healthConnectService.hasPermissions()
        }
    }

    fun startPolling(rideStartMs: Long) {
        this.rideStartMs = rideStartMs
        polling = true
        viewModelScope.launch {
            while (polling) {
                val hr = healthConnectService.queryLiveHeartRate(
                    rideStartMs,
                    System.currentTimeMillis(),
                )
                _currentHr.value = hr
                delay(5000)
            }
        }
    }

    fun stopPolling() {
        polling = false
    }
}
