package com.trailride.tracker.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class WeatherReading(
    val temperatureC: Double,
    val windspeedKmh: Double,
    val weatherCode: Int,
    val source: String,
)

@Singleton
class WeatherService @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun fetchCurrent(lat: Double, lon: Double): WeatherReading =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,wind_speed_10m" +
                    "&timezone=auto"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext offlineFallback()

                val json = JSONObject(body)
                val current = json.getJSONObject("current")

                WeatherReading(
                    temperatureC = current.optDouble("temperature_2m", 0.0),
                    windspeedKmh = current.optDouble("wind_speed_10m", 0.0),
                    weatherCode = current.optInt("weather_code", 0),
                    source = "open-meteo",
                )
            } catch (_: Exception) {
                offlineFallback()
            }
        }

    private fun offlineFallback() = WeatherReading(
        temperatureC = 0.0,
        windspeedKmh = 0.0,
        weatherCode = 0,
        source = "offline-fallback",
    )
}
