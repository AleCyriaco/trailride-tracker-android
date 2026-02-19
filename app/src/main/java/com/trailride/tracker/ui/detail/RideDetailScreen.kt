package com.trailride.tracker.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailride.tracker.viewmodel.RideDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    rideId: Long,
    onBack: () -> Unit,
    viewModel: RideDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.ride?.trailName ?: "Ride Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        renameText = detail?.ride?.trailName ?: ""
                        showRenameDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (detail != null) {
            val d = detail!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { RideSummaryCard(ride = d.ride) }

                if (d.trackPoints.isNotEmpty()) {
                    item { ElevationChart(trackPoints = d.trackPoints) }
                }

                if (d.healthSamples.isNotEmpty()) {
                    item { HeartRateChart(samples = d.healthSamples) }
                }

                if (d.healthSummary != null) {
                    item { HealthCard(summary = d.healthSummary!!) }
                }

                if (d.weatherSnapshots.isNotEmpty()) {
                    item { WeatherCard(snapshots = d.weatherSnapshots) }
                }

                item {
                    ExportCard(
                        onExportGPX = {
                            viewModel.exportGPX()?.let { uri ->
                                val intent = viewModel.createShareIntent(uri, "application/gpx+xml")
                                context.startActivity(Intent.createChooser(intent, "Share"))
                            }
                        },
                        onExportGeoJSON = {
                            viewModel.exportGeoJSON()?.let { uri ->
                                val intent = viewModel.createShareIntent(uri, "application/json")
                                context.startActivity(Intent.createChooser(intent, "Share"))
                            }
                        },
                        onExportCSV = {
                            viewModel.exportCSV()?.let { uri ->
                                val intent = viewModel.createShareIntent(uri, "text/csv")
                                context.startActivity(Intent.createChooser(intent, "Share"))
                            }
                        },
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Ride") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Trail Name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameRide(renameText)
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
