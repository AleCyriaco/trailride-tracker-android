package com.trailride.tracker.data.db.entity

data class RideDetail(
    val ride: RideEntity,
    val trackPoints: List<TrackPointEntity>,
    val stops: List<StopEntity>,
    val weatherSnapshots: List<WeatherSnapshotEntity>,
    val sensorSamples: List<SensorSampleEntity>,
    val healthSummary: HealthSummaryEntity?,
    val healthSamples: List<HealthSampleEntity>,
)
