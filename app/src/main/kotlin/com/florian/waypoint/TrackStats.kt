package com.florian.waypoint

import org.osmdroid.util.GeoPoint
import kotlin.math.max

data class TrackStatistics(
    val distanceMeters: Double,
    val durationMs: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGain: Double?,
    val elevationLoss: Double?,
    val verticalDescended: Double?,
    val runCount: Int
)

fun computeTrackStats(points: List<TrackPoint>): TrackStatistics {
    if (points.size < 2) return TrackStatistics(0.0, 0, 0.0, 0.0, null, null, null, 0)

    var totalDist = 0.0
    var gain = 0.0
    var loss = 0.0
    var maxSpeed = 0.0
    var hasElevation = false

    for (i in 1 until points.size) {
        val a = points[i - 1]; val b = points[i]
        val segDist = distanceMeters(
            GeoPoint(a.latitude, a.longitude),
            GeoPoint(b.latitude, b.longitude)
        )
        totalDist += segDist

        // Instantaneous speed (excludes lift segments)
        val dtMs = b.timestamp - a.timestamp
        if (dtMs in 500..30000) {
            val speedMs = segDist / (dtMs / 1000.0)
            // Only count as "max speed" if not on a lift (horizontal speed > 2 m/s)
            if (speedMs in 2.0..60.0) { // cap at 60 m/s ≈ 216 km/h to filter GPS jumps
                maxSpeed = max(maxSpeed, speedMs * 3.6)
            }
        }

        if (a.altitude != null && b.altitude != null && a.altitude != 0.0 && b.altitude != 0.0) {
            hasElevation = true
            val diff = b.altitude - a.altitude
            if (diff > 0) gain += diff else loss -= diff
        }
    }

    val duration = points.last().timestamp - points.first().timestamp
    val hours = duration / 3_600_000.0
    val avgSpeed = if (hours > 0) (totalDist / 1000.0) / hours else 0.0

    val runs = if (hasElevation) detectRuns(points).size else 0
    val verticalDescended = if (hasElevation) detectRuns(points).sumOf { it.verticalDrop } else null

    return TrackStatistics(
        distanceMeters = totalDist,
        durationMs = duration,
        avgSpeedKmh = avgSpeed,
        maxSpeedKmh = maxSpeed,
        elevationGain = if (hasElevation) gain else null,
        elevationLoss = if (hasElevation) loss else null,
        verticalDescended = verticalDescended,
        runCount = runs
    )
}

data class SkiRun(val startIndex: Int, val endIndex: Int, val verticalDrop: Double)

/**
 * Detect individual ski runs from a track.
 *
 * A "run" is a continuous descent of at least 30 meters of vertical drop.
 * Lifts (rising elevation with low horizontal speed) and flats break runs apart.
 */
fun detectRuns(points: List<TrackPoint>): List<SkiRun> {
    if (points.size < 5) return emptyList()

    val runs = mutableListOf<SkiRun>()
    var runStart: Int? = null
    var runStartAlt = 0.0
    var runMinAlt = 0.0

    for (i in 1 until points.size) {
        val prev = points[i - 1]; val curr = points[i]
        val prevAlt = prev.altitude ?: 0.0
        val currAlt = curr.altitude ?: 0.0
        if (prevAlt == 0.0 || currAlt == 0.0) continue

        val dAlt = currAlt - prevAlt // negative when going down

        if (dAlt < -0.5) {
            // Descending
            if (runStart == null) {
                runStart = i - 1
                runStartAlt = prevAlt
                runMinAlt = currAlt
            } else {
                if (currAlt < runMinAlt) runMinAlt = currAlt
            }
        } else if (dAlt > 1.0) {
            // Climbing — end any current run
            if (runStart != null) {
                val drop = runStartAlt - runMinAlt
                if (drop >= 30.0) runs.add(SkiRun(runStart, i, drop))
                runStart = null
            }
        }
        // Flat (-0.5 to 1.0) keeps the run open
    }

    // Final open run
    if (runStart != null) {
        val drop = runStartAlt - runMinAlt
        if (drop >= 30.0) runs.add(SkiRun(runStart!!, points.size - 1, drop))
    }

    return runs
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

fun formatTrackSpeed(kmh: Double): String = "%.1f km/h".format(kmh)

fun formatVertical(meters: Double, imperial: Boolean): String =
    if (imperial) "${(meters * 3.28084).toInt()} ft" else "${meters.toInt()} m"
