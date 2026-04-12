package com.florian.piste

import java.util.UUID

/**
 * A user-placed waypoint on the map.
 *
 * @property id Stable UUID; used for selection and photo file naming.
 * @property color Hex color string for the pin — picked from a preset palette.
 * @property photoPath Absolute path to an attached photo in app internal storage, or null.
 * @property icon Key of one of the [WaypointPresetIcons], or null for the default pin.
 */
data class Waypoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val color: String = "#3C3734",
    val photoPath: String? = null,
    val icon: String? = null
)

/** A single GPS sample in a recorded track. */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double? = null
)

/**
 * A recorded ski track consisting of a list of GPS points plus aggregate metadata.
 *
 * @property biggestAirMs Longest air-time event detected during recording, in ms.
 * @property airCount Total number of air-time events detected during recording.
 */
data class Track(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val points: List<TrackPoint>,
    val startTime: Long,
    val endTime: Long,
    val color: String = "#007AFF",
    val biggestAirMs: Long = 0,
    val airCount: Int = 0
)
