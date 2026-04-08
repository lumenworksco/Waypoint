package com.florian.waypoint

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WaypointStore(context: Context) {
    private val prefs = context.getSharedPreferences("waypoints", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): List<Waypoint> {
        val json = prefs.getString("waypoints_json", null) ?: return emptyList()
        val type = object : TypeToken<List<Waypoint>>() {}.type
        return gson.fromJson(json, type)
    }

    fun save(waypoints: List<Waypoint>) {
        prefs.edit().putString("waypoints_json", gson.toJson(waypoints)).apply()
    }
}
