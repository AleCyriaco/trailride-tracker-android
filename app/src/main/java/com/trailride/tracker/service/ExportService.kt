package com.trailride.tracker.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.trailride.tracker.data.db.entity.RideDetail
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun generateGPX(detail: RideDetail): Uri? {
        val ride = detail.ride
        val points = detail.trackPoints

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="TrailRide Tracker" xmlns="http://www.topografix.com/GPX/1/1">""")
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${escapeXml(ride.trailName)}</name>")
        sb.appendLine("    <time>${isoFormat.format(Date(ride.startTs))}</time>")
        sb.appendLine("  </metadata>")
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${escapeXml(ride.trailName)}</name>")
        sb.appendLine("    <trkseg>")

        for (point in points) {
            sb.appendLine("""      <trkpt lat="${point.lat}" lon="${point.lon}">""")
            point.altitude?.let {
                sb.appendLine("        <ele>${String.format(Locale.US, "%.1f", it)}</ele>")
            }
            sb.appendLine("        <time>${isoFormat.format(Date(point.ts))}</time>")
            sb.appendLine("      </trkpt>")
        }

        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")

        return writeToCache(sb.toString(), "${sanitizeFilename(ride.trailName)}.gpx")
    }

    fun generateGeoJSON(detail: RideDetail): Uri? {
        val ride = detail.ride
        val points = detail.trackPoints

        val coordinates = points.joinToString(",\n        ") { p ->
            if (p.altitude != null) {
                "[${p.lon}, ${p.lat}, ${p.altitude}]"
            } else {
                "[${p.lon}, ${p.lat}]"
            }
        }

        val endTs = ride.endTs?.let { "\"${isoFormat.format(Date(it))}\"" } ?: "null"

        val json = """
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "ride_id": ${ride.id},
        "trail_name": "${ride.trailName.replace("\"", "\\\"")}",
        "start_ts": "${isoFormat.format(Date(ride.startTs))}",
        "end_ts": $endTs,
        "distance_meters": ${ride.distanceMeters}
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
        $coordinates
        ]
      }
    }
  ]
}
""".trimIndent()

        return writeToCache(json, "${sanitizeFilename(ride.trailName)}.geojson")
    }

    fun generateCSV(detail: RideDetail): Uri? {
        val ride = detail.ride
        val points = detail.trackPoints

        val sb = StringBuilder()
        sb.appendLine("timestamp,latitude,longitude,altitude,accuracy,speed,segment_distance")
        for (p in points) {
            sb.appendLine("${p.ts},${p.lat},${p.lon},${p.altitude ?: ""},${p.accuracy ?: ""},${p.speed ?: ""},${p.segmentDistance}")
        }

        return writeToCache(sb.toString(), "${sanitizeFilename(ride.trailName)}.csv")
    }

    fun createShareIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeToCache(content: String, filename: String): Uri? {
        return try {
            val dir = File(context.cacheDir, "exports")
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeText(content)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9\\-_ ]"), "").replace(" ", "_")
}
