package com.trailride.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailride.tracker.data.db.entity.RideEntity
import com.trailride.tracker.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val repository: RideRepository,
) : ViewModel() {

    private val _rides = MutableStateFlow<List<RideEntity>>(emptyList())
    val rides: StateFlow<List<RideEntity>> = _rides.asStateFlow()

    private val _selectedRideA = MutableStateFlow<Long?>(null)
    val selectedRideA: StateFlow<Long?> = _selectedRideA.asStateFlow()

    private val _selectedRideB = MutableStateFlow<Long?>(null)
    val selectedRideB: StateFlow<Long?> = _selectedRideB.asStateFlow()

    private val _rideA = MutableStateFlow<RideEntity?>(null)
    val rideA: StateFlow<RideEntity?> = _rideA.asStateFlow()

    private val _rideB = MutableStateFlow<RideEntity?>(null)
    val rideB: StateFlow<RideEntity?> = _rideB.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _rides.value = repository.getCompletedRides()
        }
    }

    fun selectRideA(id: Long?) {
        _selectedRideA.value = id
        _rideA.value = id?.let { rideId -> _rides.value.find { it.id == rideId } }
    }

    fun selectRideB(id: Long?) {
        _selectedRideB.value = id
        _rideB.value = id?.let { rideId -> _rides.value.find { it.id == rideId } }
    }
}
