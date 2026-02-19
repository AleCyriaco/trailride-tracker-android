package com.trailride.tracker.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object WeatherCodeMapper {

    /** Maps WMO weather code to Material icon. */
    fun icon(code: Int): ImageVector = when (code) {
        0 -> Icons.Default.WbSunny                // Clear sky
        1 -> Icons.Default.WbSunny                // Mainly clear
        2 -> Icons.Default.WbCloudy               // Partly cloudy
        3 -> Icons.Default.Cloud                   // Overcast
        45, 48 -> Icons.Default.Dehaze             // Fog
        in 51..57 -> Icons.Default.Grain           // Drizzle
        in 61..67 -> Icons.Default.Opacity         // Rain
        in 71..77 -> Icons.Default.AcUnit          // Snow
        in 80..82 -> Icons.Default.Opacity         // Rain showers
        in 85..86 -> Icons.Default.AcUnit          // Snow showers
        95 -> Icons.Default.FlashOn                // Thunderstorm
        96, 99 -> Icons.Default.FlashOn            // Thunderstorm with hail
        else -> Icons.Default.Help
    }

    /** Maps WMO weather code to description. */
    fun description(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45 -> "Fog"
        48 -> "Rime fog"
        51 -> "Light drizzle"
        53 -> "Moderate drizzle"
        55 -> "Dense drizzle"
        56 -> "Light freezing drizzle"
        57 -> "Dense freezing drizzle"
        61 -> "Slight rain"
        63 -> "Moderate rain"
        65 -> "Heavy rain"
        66 -> "Light freezing rain"
        67 -> "Heavy freezing rain"
        71 -> "Slight snow"
        73 -> "Moderate snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Slight showers"
        81 -> "Moderate showers"
        82 -> "Violent showers"
        85 -> "Slight snow showers"
        86 -> "Heavy snow showers"
        95 -> "Thunderstorm"
        96 -> "Thunderstorm, slight hail"
        99 -> "Thunderstorm, heavy hail"
        else -> "Unknown"
    }
}
