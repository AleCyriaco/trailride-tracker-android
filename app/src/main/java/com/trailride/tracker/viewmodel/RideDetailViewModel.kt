package com.trailride.tracker.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailride.tracker.data.db.entity.RideDetail
import com.trailride.tracker.data.repository.RideRepository
import com.trailride.tracker.service.ExportService
import com.trailride.tracker.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RideRepository,
    private val exportService: ExportService,
    private val healthConnectService: HealthConnectService,
) : ViewModel() {

    private val rideId: Long = savedStateHandle["rideId"] ?: 0L

    private val _detail = MutableStateFlow<RideDetail?>(null)
    val detail: StateFlow<RideDetail?> = _detail.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _detail.value = repository.getRideDetail(rideId)
            _isLoading.value = false
        }
    }

    fun renameRide(newName: String) {
        viewModelScope.launch {
            val ride = _detail.value?.ride ?: return@launch
            val updated = ride.copy(trailName = newName)
            repository.updateRide(updated)
            _detail.value = _detail.value?.copy(ride = updated)
        }
    }

    fun importHealth() {
        viewModelScope.launch {
            val ride = _detail.value?.ride ?: return@launch
            val endTs = ride.endTs ?: return@launch
            val result = healthConnectService.importForRide(ride.id, ride.startTs, endTs)
            if (result != null) {
                val (summary, samples) = result
                repository.upsertHealthSummary(summary)
                repository.insertHealthSamples(samples)
                load() // Refresh
            }
        }
    }

    fun exportGPX(): Uri? = _detail.value?.let { exportService.generateGPX(it) }

    fun exportGeoJSON(): Uri? = _detail.value?.let { exportService.generateGeoJSON(it) }

    fun exportCSV(): Uri? = _detail.value?.let { exportService.generateCSV(it) }

    fun createShareIntent(uri: Uri, mimeType: String) =
        exportService.createShareIntent(uri, mimeType)
}
