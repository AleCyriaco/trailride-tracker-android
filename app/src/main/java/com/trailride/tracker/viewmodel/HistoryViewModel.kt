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
class HistoryViewModel @Inject constructor(
    private val repository: RideRepository,
) : ViewModel() {

    private val _rides = MutableStateFlow<List<RideEntity>>(emptyList())
    val rides: StateFlow<List<RideEntity>> = _rides.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredRides = MutableStateFlow<List<RideEntity>>(emptyList())
    val filteredRides: StateFlow<List<RideEntity>> = _filteredRides.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            repository.getAllRidesFlow().collect { rides ->
                _rides.value = rides
                applyFilter()
            }
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteRide(id: Long) {
        viewModelScope.launch {
            repository.deleteRide(id)
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.lowercase()
        _filteredRides.value = if (query.isBlank()) {
            _rides.value
        } else {
            _rides.value.filter { it.trailName.lowercase().contains(query) }
        }
    }
}
