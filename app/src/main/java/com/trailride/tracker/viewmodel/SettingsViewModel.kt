package com.trailride.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailride.tracker.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: RideRepository,
) : ViewModel() {

    private val _storeHealthSamples = MutableStateFlow(true)
    val storeHealthSamples: StateFlow<Boolean> = _storeHealthSamples.asStateFlow()

    private val _maxHr = MutableStateFlow(190)
    val maxHr: StateFlow<Int> = _maxHr.asStateFlow()

    private val _minAccuracy = MutableStateFlow(30.0)
    val minAccuracy: StateFlow<Double> = _minAccuracy.asStateFlow()

    private val _autoStopDelay = MutableStateFlow(15)
    val autoStopDelay: StateFlow<Int> = _autoStopDelay.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            repository.getSetting("store_health_samples")?.let {
                _storeHealthSamples.value = it == "true"
            }
            repository.getSetting("max_hr")?.let { v ->
                v.toIntOrNull()?.let { _maxHr.value = it }
            }
            repository.getSetting("min_accuracy")?.let { v ->
                v.toDoubleOrNull()?.let { _minAccuracy.value = it }
            }
            repository.getSetting("auto_stop_delay")?.let { v ->
                v.toIntOrNull()?.let { _autoStopDelay.value = it }
            }
        }
    }

    fun setStoreHealthSamples(value: Boolean) {
        _storeHealthSamples.value = value
        save("store_health_samples", value.toString())
    }

    fun setMaxHr(value: Int) {
        _maxHr.value = value
        save("max_hr", value.toString())
    }

    fun setMinAccuracy(value: Double) {
        _minAccuracy.value = value
        save("min_accuracy", value.toString())
    }

    fun setAutoStopDelay(value: Int) {
        _autoStopDelay.value = value
        save("auto_stop_delay", value.toString())
    }

    private fun save(key: String, value: String) {
        viewModelScope.launch {
            repository.setSetting(key, value)
        }
    }
}
