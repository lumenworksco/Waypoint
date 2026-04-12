package com.florian.piste

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persistent storage for all app data backed by a single SharedPreferences file.
 *
 * Data is serialized as JSON via Gson for waypoints and tracks, and as simple
 * key-value strings for settings. Kept deliberately simple — no Room, no DAOs,
 * no DI. The entire store is a thin wrapper around SharedPreferences.
 */
class WaypointStore(context: Context) {
    private val prefs = context.getSharedPreferences("waypoints", Context.MODE_PRIVATE)
    private val gson = Gson()

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

    fun loadTracks(): List<Track> {
        val json = prefs.getString("tracks_json", null) ?: return emptyList()
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveTracks(tracks: List<Track>) {
        prefs.edit().putString("tracks_json", gson.toJson(tracks)).apply()
    }

    fun loadMapStyle(): String = prefs.getString("map_style", "STANDARD") ?: "STANDARD"
    fun saveMapStyle(style: String) { prefs.edit().putString("map_style", style).apply() }

    fun loadCoordFormat(): String = prefs.getString("coord_format", "DECIMAL") ?: "DECIMAL"
    fun saveCoordFormat(format: String) { prefs.edit().putString("coord_format", format).apply() }

    // Generic settings
    fun loadSetting(key: String, default: String): String = prefs.getString("setting_$key", default) ?: default
    fun saveSetting(key: String, value: String) { prefs.edit().putString("setting_$key", value).apply() }

    // Home waypoint
    fun loadHomeId(): String? = prefs.getString("home_waypoint_id", null)
    fun saveHomeId(id: String?) {
        prefs.edit().apply {
            if (id == null) remove("home_waypoint_id") else putString("home_waypoint_id", id)
        }.apply()
    }
}
