package com.florian.piste

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PisteRepository(context: Context) {
    private val store = WaypointStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _waypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val waypoints: StateFlow<List<Waypoint>> = _waypoints.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    init {
        scope.launch {
            _waypoints.value = store.load()
            _tracks.value = store.loadTracks()
        }
    }

    // Waypoint operations
    fun addWaypoint(waypoint: Waypoint) {
        _waypoints.value = _waypoints.value + waypoint
        scope.launch { store.save(_waypoints.value) }
    }

    fun updateWaypoint(updated: Waypoint) {
        _waypoints.value = _waypoints.value.map { if (it.id == updated.id) updated else it }
        scope.launch { store.save(_waypoints.value) }
    }

    fun deleteWaypoint(id: String): Waypoint? {
        val deleted = _waypoints.value.find { it.id == id }
        _waypoints.value = _waypoints.value.filter { it.id != id }
        scope.launch { store.save(_waypoints.value) }
        return deleted
    }

    fun restoreWaypoint(waypoint: Waypoint) {
        _waypoints.value = _waypoints.value + waypoint
        scope.launch { store.save(_waypoints.value) }
    }

    fun importWaypoints(newWaypoints: List<Waypoint>) {
        _waypoints.value = _waypoints.value + newWaypoints
        scope.launch { store.save(_waypoints.value) }
    }

    // Track operations
    fun addTrack(track: Track) {
        _tracks.value = _tracks.value + track
        scope.launch { store.saveTracks(_tracks.value) }
    }

    fun updateTrack(updated: Track) {
        _tracks.value = _tracks.value.map { if (it.id == updated.id) updated else it }
        scope.launch { store.saveTracks(_tracks.value) }
    }

    fun deleteTrack(id: String) {
        _tracks.value = _tracks.value.filter { it.id != id }
        scope.launch { store.saveTracks(_tracks.value) }
    }

    fun importTracks(newTracks: List<Track>) {
        _tracks.value = _tracks.value + newTracks
        scope.launch { store.saveTracks(_tracks.value) }
    }

    // Settings
    fun loadSetting(key: String, default: String): String = store.loadSetting(key, default)
    fun saveSetting(key: String, value: String) { store.saveSetting(key, value) }
    fun loadMapStyle(): String = store.loadMapStyle()
    fun saveMapStyle(style: String) { store.saveMapStyle(style) }
    fun loadHomeId(): String? = store.loadHomeId()
    fun saveHomeId(id: String?) { store.saveHomeId(id) }

    // Achievements
    fun checkAchievements(maxSpeed: Double, dayVert: Double, dayRuns: Int, dayDist: Double): List<String> =
        store.checkAchievements(maxSpeed, dayVert, dayRuns, dayDist)

    // Reload from disk (after backup restore)
    fun reload() {
        _waypoints.value = store.load()
        _tracks.value = store.loadTracks()
    }

    // Direct store access for backup/export operations
    val directStore: WaypointStore get() = store
}
