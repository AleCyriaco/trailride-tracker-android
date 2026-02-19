package com.trailride.tracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    /** Format seconds into "H:MM:SS" or "M:SS". */
    fun formatDuration(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    /** Format meters as "12.45 km" or "450 m". */
    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(Locale.US, "%.2f km", meters / 1000)
        } else {
            String.format(Locale.US, "%.0f m", meters)
        }
    }

    /** Format m/s to "42.5 km/h". */
    fun formatSpeedKmh(mps: Double): String {
        return String.format(Locale.US, "%.1f km/h", mps * 3.6)
    }

    /** Format elevation in meters. */
    fun formatElevation(meters: Double): String {
        return String.format(Locale.US, "%.0f m", meters)
    }

    /** Format ms epoch to "dd/MM/yyyy HH:mm". */
    fun formatDateTime(ms: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(ms))
    }

    /** Convert ms epoch to Date. */
    fun dateFromMs(ms: Long): Date = Date(ms)
}
