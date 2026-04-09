package com.florian.waypoint

import org.osmdroid.util.GeoPoint

data class TrackStatistics(
    val distanceMeters: Double,
    val durationMs: Long,
    val avgSpeedKmh: Double,
    val elevationGain: Double?,
    val elevationLoss: Double?
)

fun computeTrackStats(points: List<TrackPoint>): TrackStatistics {
    if (points.size < 2) return TrackStatistics(0.0, 0, 0.0, null, null)

    var totalDist = 0.0
    var gain = 0.0
    var loss = 0.0
    var hasElevation = false

    for (i in 1 until points.size) {
        val a = points[i - 1]; val b = points[i]
        totalDist += distanceMeters(
            GeoPoint(a.latitude, a.longitude),
            GeoPoint(b.latitude, b.longitude)
        )
        if (a.altitude != null && b.altitude != null && a.altitude != 0.0 && b.altitude != 0.0) {
            hasElevation = true
            val diff = b.altitude - a.altitude
            if (diff > 0) gain += diff else loss -= diff
        }
    }

    val duration = points.last().timestamp - points.first().timestamp
    val hours = duration / 3_600_000.0
    val speed = if (hours > 0) (totalDist / 1000.0) / hours else 0.0

    return TrackStatistics(
        distanceMeters = totalDist,
        durationMs = duration,
        avgSpeedKmh = speed,
        elevationGain = if (hasElevation) gain else null,
        elevationLoss = if (hasElevation) loss else null
    )
}

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

fun formatSpeed(kmh: Double): String = "%.1f km/h".format(kmh)
