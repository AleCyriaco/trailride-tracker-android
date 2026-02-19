package com.trailride.tracker.ui.liveride

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.trailride.tracker.service.TrackingService
import com.trailride.tracker.ui.theme.TrailGreen
import com.trailride.tracker.viewmodel.LiveRideViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_ID = "route-source"
private const val LAYER_ID = "route-layer"

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

    // Hold references for the MapLibre map and style
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    // Update polyline + camera whenever trackPoints change
    LaunchedEffect(trackPoints, styleReady) {
        val map = mapRef ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        if (trackPoints.size >= 2) {
            val lineString = LineString.fromLngLats(
                trackPoints.map { Point.fromLngLat(it.lon, it.lat) },
            )
            val existing = style.getSource(SOURCE_ID) as? GeoJsonSource
            if (existing != null) {
                existing.setGeoJson(lineString)
            } else {
                style.addSource(GeoJsonSource(SOURCE_ID, lineString))
                style.addLayer(
                    LineLayer(LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.lineColor(TrailGreen.toArgb()),
                        PropertyFactory.lineWidth(5f),
                    ),
                )
            }
        }

        if (trackPoints.isNotEmpty()) {
            val last = trackPoints.last()
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(last.lat, last.lon))
                        .zoom(15.0)
                        .build(),
                ),
            )
        }
    }

    // Lifecycle management for MapView
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mv.onStart()
                Lifecycle.Event.ON_RESUME -> mv.onResume()
                Lifecycle.Event.ON_PAUSE -> mv.onPause()
                Lifecycle.Event.ON_STOP -> mv.onStop()
                Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map at top — fixed height, outside scroll
        AndroidView(
            factory = { context ->
                MapLibre.getInstance(context)
                MapView(context).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        map.setStyle(Style.Builder().fromUri(STYLE_URL)) { _ ->
                            mapRef = map
                            styleReady = true
                        }
                    }
                    mapViewRef.value = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
        )

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
                enabled = state == com.trailride.tracker.service.TrackingState.IDLE,
            )

            Spacer(Modifier.height(8.dp))

            // Metrics grid
            MetricsGrid(metrics = metrics)

            Spacer(Modifier.height(8.dp))

            // BLE sensor panel
            if (state != com.trailride.tracker.service.TrackingState.IDLE) {
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
