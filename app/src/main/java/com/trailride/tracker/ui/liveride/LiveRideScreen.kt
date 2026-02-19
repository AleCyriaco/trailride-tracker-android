package com.trailride.tracker.ui.liveride

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trailride.tracker.service.TrackingState
import com.trailride.tracker.ui.theme.TrailGreen
import com.trailride.tracker.viewmodel.LiveRideViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveRideScreen(
    viewModel: LiveRideViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val trackPoints by viewModel.trackPoints.collectAsState()
    val trailName by viewModel.trailName.collectAsState()

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    val mapLatLngs = trackPoints.map { LatLng(it.lat, it.lon) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            if (mapLatLngs.isNotEmpty()) mapLatLngs.last()
            else LatLng(0.0, 0.0),
            15f,
        )
    }

    // Update camera when new points arrive
    LaunchedEffect(trackPoints) {
        if (mapLatLngs.isNotEmpty()) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                mapLatLngs.last(), 15f,
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map at top — fixed height, outside scroll
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
        ) {
            if (mapLatLngs.size >= 2) {
                Polyline(
                    points = mapLatLngs,
                    color = TrailGreen,
                    width = 5f,
                )
            }
        }

        // Scrollable content below map
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Trail name field
            OutlinedTextField(
                value = trailName,
                onValueChange = { viewModel.updateTrailName(it) },
                label = { Text("Trail Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                enabled = state == TrackingState.IDLE,
            )

            Spacer(Modifier.height(8.dp))

            // Metrics grid
            MetricsGrid(metrics = metrics)

            Spacer(Modifier.height(8.dp))

            // BLE sensor panel
            if (state != TrackingState.IDLE) {
                BleSensorPanel()
                Spacer(Modifier.height(8.dp))
            }

            // Controls
            RideControls(
                state = state,
                onStart = { viewModel.startRide() },
                onPause = { viewModel.pauseRide() },
                onResume = { viewModel.resumeRide() },
                onStop = { viewModel.stopRide() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
