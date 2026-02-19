package com.trailride.tracker.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.trailride.tracker.data.db.entity.TrackPointEntity
import com.trailride.tracker.ui.theme.TrailGreen
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun RouteMapCard(trackPoints: List<TrackPointEntity>) {
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

    Card(modifier = Modifier.fillMaxWidth()) {
        AndroidView(
            factory = { context ->
                MapLibre.getInstance(context)
                MapView(context).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                            val points = trackPoints.map { Point.fromLngLat(it.lon, it.lat) }
                            val lineString = LineString.fromLngLats(points)
                            style.addSource(GeoJsonSource("route", lineString))
                            style.addLayer(
                                LineLayer("route-layer", "route").withProperties(
                                    PropertyFactory.lineColor(TrailGreen.toArgb()),
                                    PropertyFactory.lineWidth(5f),
                                ),
                            )

                            // Fit camera to route bounds with padding
                            val boundsBuilder = LatLngBounds.Builder()
                            trackPoints.forEach {
                                boundsBuilder.include(
                                    org.maplibre.android.geometry.LatLng(it.lat, it.lon),
                                )
                            }
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 48),
                            )
                        }
                    }
                    mapViewRef.value = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
}
