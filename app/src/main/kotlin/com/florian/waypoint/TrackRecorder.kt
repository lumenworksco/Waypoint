package com.florian.waypoint

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackRecorder {
    var isRecording by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var currentPoints by mutableStateOf<List<TrackPoint>>(emptyList())
        private set
    private var startTime: Long = 0

    fun start() {
        isRecording = true
        isPaused = false
        currentPoints = emptyList()
        startTime = System.currentTimeMillis()
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    fun addPoint(lat: Double, lon: Double, altitude: Double? = null) {
        if (!isRecording || isPaused) return
        currentPoints = currentPoints + TrackPoint(lat, lon, System.currentTimeMillis(), altitude)
    }

    fun stop(): Track {
        isRecording = false
        isPaused = false
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        return Track(
            name = "Track ${fmt.format(Date(startTime))}",
            points = currentPoints,
            startTime = startTime,
            endTime = System.currentTimeMillis()
        )
    }

    fun discard() {
        isRecording = false
        isPaused = false
        currentPoints = emptyList()
    }
}
