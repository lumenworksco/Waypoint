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

        // Instantaneous speed (excludes lift segments and GPS glitches)
        val dtMs = b.timestamp - a.timestamp
        if (dtMs in 500..30000) {
            val speedMs = segDist / (dtMs / 1000.0)
            // 2 m/s lower bound filters lift rides; 40 m/s (= 144 km/h) upper bound filters GPS jumps
            if (speedMs in 2.0..40.0) {
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

// ── Per-run breakdown ───────────────────────────────────────────

enum class RunDifficulty(val label: String) { EASY("Easy"), MODERATE("Moderate"), STEEP("Steep") }

data class RunBreakdown(
    val index: Int,
    val startTime: Long,
    val endTime: Long,
    val verticalDrop: Double,
    val distanceMeters: Double,
    val maxSpeedKmh: Double,
    val avgSpeedKmh: Double,
    val difficulty: RunDifficulty,
    val gradient: Double,
)

fun computeRunBreakdown(points: List<TrackPoint>): List<RunBreakdown> {
    val runs = detectRuns(points)
    return runs.mapIndexed { idx, run ->
        val segment = points.subList(run.startIndex, minOf(run.endIndex + 1, points.size))
        var dist = 0.0
        var runMax = 0.0
        for (i in 1 until segment.size) {
            val a = segment[i - 1]; val b = segment[i]
            val d = distanceMeters(GeoPoint(a.latitude, a.longitude), GeoPoint(b.latitude, b.longitude))
            dist += d
            val dt = b.timestamp - a.timestamp
            if (dt in 500..30000) {
                val kmh = (d / (dt / 1000.0)) * 3.6
                // Filter lift rides (< 7 km/h) and GPS jumps (> 144 km/h)
                if (kmh in 7.0..144.0) runMax = max(runMax, kmh)
            }
        }
        val duration = segment.last().timestamp - segment.first().timestamp
        val avgKmh = if (duration > 0) (dist / 1000.0) / (duration / 3_600_000.0) else 0.0
        val gradient = if (dist > 0) run.verticalDrop / dist else 0.0
        val difficulty = when {
            gradient < 0.15 -> RunDifficulty.EASY
            gradient < 0.30 -> RunDifficulty.MODERATE
            else -> RunDifficulty.STEEP
        }
        RunBreakdown(
            index = idx + 1,
            startTime = segment.first().timestamp,
            endTime = segment.last().timestamp,
            verticalDrop = run.verticalDrop,
            distanceMeters = dist,
            maxSpeedKmh = runMax,
            avgSpeedKmh = avgKmh,
            difficulty = difficulty,
            gradient = gradient
        )
    }
}

// ── Time on snow vs lifts ───────────────────────────────────────

data class TimeOnSnow(val snowMs: Long, val liftMs: Long) {
    val snowPercent: Int get() {
        val total = snowMs + liftMs
        return if (total > 0) ((snowMs * 100) / total).toInt() else 0
    }
}

fun computeTimeOnSnow(points: List<TrackPoint>): TimeOnSnow {
    if (points.size < 2) return TimeOnSnow(0, 0)
    val runs = detectRuns(points)
    var snowMs = 0L
    for (r in runs) {
        snowMs += points[minOf(r.endIndex, points.size - 1)].timestamp - points[r.startIndex].timestamp
    }
    val total = points.last().timestamp - points.first().timestamp
    return TimeOnSnow(snowMs = snowMs, liftMs = (total - snowMs).coerceAtLeast(0))
}

// ── Speed heatmap color ─────────────────────────────────────────

/** Maps speed (km/h) to a color from blue (slow) through green, yellow, to red (fast). */
fun speedToColor(kmh: Double): Int {
    val clamped = kmh.coerceIn(0.0, 80.0) / 80.0
    return when {
        clamped < 0.33 -> {
            val t = clamped / 0.33
            interp(0x2E7DFF, 0x34C759, t.toFloat())
        }
        clamped < 0.66 -> {
            val t = (clamped - 0.33) / 0.33
            interp(0x34C759, 0xFFCC00, t.toFloat())
        }
        else -> {
            val t = (clamped - 0.66) / 0.34
            interp(0xFFCC00, 0xFF3B30, t.toFloat())
        }
    }
}

private fun interp(c1: Int, c2: Int, t: Float): Int {
    val r1 = (c1 shr 16) and 0xFF; val g1 = (c1 shr 8) and 0xFF; val b1 = c1 and 0xFF
    val r2 = (c2 shr 16) and 0xFF; val g2 = (c2 shr 8) and 0xFF; val b2 = c2 and 0xFF
    val r = (r1 + (r2 - r1) * t).toInt()
    val g = (g1 + (g2 - g1) * t).toInt()
    val b = (b1 + (b2 - b1) * t).toInt()
    return android.graphics.Color.argb(255, r, g, b)
}

/** Returns the hex color for a run difficulty (green/blue/black). */
fun difficultyColor(d: RunDifficulty): Int = when (d) {
    RunDifficulty.EASY -> 0xFF34C759.toInt()
    RunDifficulty.MODERATE -> 0xFF007AFF.toInt()
    RunDifficulty.STEEP -> 0xFF1C1C1E.toInt()
}

// ── Streak counter ──────────────────────────────────────────────

/**
 * Count consecutive days ending today that have at least one recorded track.
 * Returns 0 if no track was recorded today.
 */
fun computeStreak(tracks: List<Track>): Int {
    if (tracks.isEmpty()) return 0
    val cal = java.util.Calendar.getInstance()
    fun startOfDay(ms: Long): Long {
        cal.timeInMillis = ms
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val days = tracks.map { startOfDay(it.startTime) }.toSet()
    var count = 0
    var day = startOfDay(System.currentTimeMillis())
    while (day in days) {
        count++
        day -= 86_400_000L
    }
    return count
}

// ── Session time range ─────────────────────────────────────────

fun formatTimeOfDay(ms: Long): String {
    val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(ms))
}

// ── Monthly breakdown ───────────────────────────────────────────

data class MonthStats(
    val year: Int,
    val month: Int, // 1..12
    val label: String, // "January 2026"
    val days: Int,
    val runs: Int,
    val vertical: Double,
    val distance: Double,
    val maxSpeedKmh: Double
)

/**
 * Aggregate tracks by calendar month, newest first.
 * Only returns months that have at least one track.
 */
fun computeMonthlyBreakdown(tracks: List<Track>): List<MonthStats> {
    if (tracks.isEmpty()) return emptyList()

    val cal = java.util.Calendar.getInstance()
    val monthFmt = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())

    // Group by year-month key
    val groups = mutableMapOf<Pair<Int, Int>, MutableList<Track>>()
    for (t in tracks) {
        cal.timeInMillis = t.startTime
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1 // 0-indexed → 1-indexed
        groups.getOrPut(year to month) { mutableListOf() }.add(t)
    }

    return groups.entries
        .sortedWith(compareByDescending<Map.Entry<Pair<Int, Int>, MutableList<Track>>> { it.key.first }.thenByDescending { it.key.second })
        .map { (key, monthTracks) ->
            val (year, month) = key
            var runs = 0; var vertical = 0.0; var distance = 0.0; var maxSpeed = 0.0
            val uniqueDays = mutableSetOf<Long>()
            for (t in monthTracks) {
                val s = computeTrackStats(t.points)
                runs += s.runCount
                vertical += s.verticalDescended ?: 0.0
                distance += s.distanceMeters
                if (s.maxSpeedKmh > maxSpeed) maxSpeed = s.maxSpeedKmh
                cal.timeInMillis = t.startTime
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
                uniqueDays.add(cal.timeInMillis)
            }
            // Build a date for the label using the first day of the month
            cal.clear()
            cal.set(year, month - 1, 1)
            MonthStats(
                year = year,
                month = month,
                label = monthFmt.format(cal.time),
                days = uniqueDays.size,
                runs = runs,
                vertical = vertical,
                distance = distance,
                maxSpeedKmh = maxSpeed
            )
        }
}
