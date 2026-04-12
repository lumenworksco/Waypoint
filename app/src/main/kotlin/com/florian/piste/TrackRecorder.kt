package com.florian.piste

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

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
    private val _currentPoints = CopyOnWriteArrayList<TrackPoint>()
    var currentPoints by mutableStateOf<List<TrackPoint>>(emptyList())
        private set
    private val _airTimes = CopyOnWriteArrayList<Long>()
    var airTimes by mutableStateOf<List<Long>>(emptyList())
        private set
    private var startTime: Long = 0

    fun start() {
        isRecording = true
        isPaused = false
        _currentPoints.clear()
        _airTimes.clear()
        currentPoints = emptyList()
        airTimes = emptyList()
        startTime = System.currentTimeMillis()
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    fun addPoint(lat: Double, lon: Double, altitude: Double? = null) {
        if (!isRecording || isPaused) return
        _currentPoints.add(TrackPoint(lat, lon, System.currentTimeMillis(), altitude))
        currentPoints = _currentPoints.toList()
    }

    fun addAirTime(durationMs: Long) {
        if (!isRecording || isPaused) return
        _airTimes.add(durationMs)
        airTimes = _airTimes.toList()
    }

    fun stop(): Track {
        isRecording = false
        isPaused = false
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        val points = _currentPoints.toList()
        val airs = _airTimes.toList()
        val biggestAir = airs.maxOrNull() ?: 0L
        val airCount = airs.size
        return Track(
            name = "Track ${fmt.format(Date(startTime))}",
            points = points,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            biggestAirMs = biggestAir,
            airCount = airCount
        )
    }

    fun discard() {
        isRecording = false
        isPaused = false
        _currentPoints.clear()
        _airTimes.clear()
        currentPoints = emptyList()
        airTimes = emptyList()
    }

    companion object {
        private const val PREFS_NAME = "piste_recovery"
        private const val KEY_ACTIVE = "recording_active"
        private const val KEY_START_TIME = "recording_start_time"

        fun markRecordingActive(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_START_TIME, System.currentTimeMillis())
                .apply()
        }

        fun markRecordingInactive(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .remove(KEY_ACTIVE)
                .remove(KEY_START_TIME)
                .apply()
        }

        fun wasRecordingInterrupted(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ACTIVE, false)
        }
    }
}
