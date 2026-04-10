package com.florian.waypoint

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Track recording state holder.
 *
 * Collects [TrackPoint]s and air-time events while recording. Exposes its state as
 * Compose [mutableStateOf] so the UI can observe recording progress directly.
 * Supports pause/resume — paused state drops incoming points and air-time events
 * but preserves the accumulated data. [stop] returns a complete [Track] with the
 * biggest air time and air-event count baked in.
 */
class TrackRecorder {
    var isRecording by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var currentPoints by mutableStateOf<List<TrackPoint>>(emptyList())
        private set
    var airTimes by mutableStateOf<List<Long>>(emptyList())
        private set
    private var startTime: Long = 0

    fun start() {
        isRecording = true
        isPaused = false
        currentPoints = emptyList()
        airTimes = emptyList()
        startTime = System.currentTimeMillis()
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    fun addPoint(lat: Double, lon: Double, altitude: Double? = null) {
        if (!isRecording || isPaused) return
        currentPoints = currentPoints + TrackPoint(lat, lon, System.currentTimeMillis(), altitude)
    }

    fun addAirTime(durationMs: Long) {
        if (!isRecording || isPaused) return
        airTimes = airTimes + durationMs
    }

    fun stop(): Track {
        isRecording = false
        isPaused = false
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        val biggestAir = airTimes.maxOrNull() ?: 0L
        val airCount = airTimes.size
        return Track(
            name = "Track ${fmt.format(Date(startTime))}",
            points = currentPoints,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            biggestAirMs = biggestAir,
            airCount = airCount
        )
    }

    fun discard() {
        isRecording = false
        isPaused = false
        currentPoints = emptyList()
        airTimes = emptyList()
    }
}
