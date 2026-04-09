package com.florian.waypoint

import java.util.UUID

data class Waypoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val color: String = "#3C3734"
)

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double? = null
)

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val points: List<TrackPoint>,
    val startTime: Long,
    val endTime: Long,
    val color: String = "#007AFF"
)
