package com.florian.waypoint

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WaypointStore(context: Context) {
    private val prefs = context.getSharedPreferences("waypoints", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Waypoints ───────────────────────────────────────────────
    fun load(): List<Waypoint> {
        val json = prefs.getString("waypoints_json", null) ?: return emptyList()
        val type = object : TypeToken<List<Waypoint>>() {}.type
        val raw: List<Waypoint> = gson.fromJson(json, type)
        @Suppress("SENSELESS_COMPARISON")
        return raw.map { if (it.color == null) it.copy(color = "#3C3734") else it }
    }

    fun save(waypoints: List<Waypoint>) {
        prefs.edit().putString("waypoints_json", gson.toJson(waypoints)).apply()
    }

    // ── Tracks ──────────────────────────────────────────────────
    fun loadTracks(): List<Track> {
        val json = prefs.getString("tracks_json", null) ?: return emptyList()
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveTracks(tracks: List<Track>) {
        prefs.edit().putString("tracks_json", gson.toJson(tracks)).apply()
    }

    // ── Map style ───────────────────────────────────────────────
    fun loadMapStyle(): String = prefs.getString("map_style", "STANDARD") ?: "STANDARD"

    fun saveMapStyle(style: String) {
        prefs.edit().putString("map_style", style).apply()
    }
}
