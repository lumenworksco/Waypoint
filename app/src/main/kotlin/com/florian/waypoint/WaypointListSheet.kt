package com.florian.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointListSheet(
    waypoints: List<Waypoint>,
    tracks: List<Track>,
    userLocation: GeoPoint?,
    onSelectWaypoint: (Waypoint) -> Unit,
    onSelectTrack: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    onReorderWaypoints: (List<Waypoint>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var manualSort by remember { mutableStateOf(false) }

    val sorted = if (manualSort) {
        waypoints
    } else if (userLocation != null) {
        waypoints.sortedBy { distanceMeters(userLocation, GeoPoint(it.latitude, it.longitude)) }
    } else {
        waypoints.sortedBy { it.name }
    }

    val filtered = if (searchQuery.isBlank()) sorted else sorted.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search waypoints") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            leadingIcon = { Icon(Icons.Filled.Search, "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, "Clear") }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header with sort toggle
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "${filtered.size} waypoint${if (filtered.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier.weight(1f)
                    )
                    if (waypoints.size > 1 && searchQuery.isBlank()) {
                        TextButton(onClick = { manualSort = !manualSort }) {
                            Text(if (manualSort) "Auto sort" else "Manual order", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                item { Text("No matches", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp)) }
            }

            items(filtered, key = { it.id }) { wp ->
                val idx = waypoints.indexOf(wp)
                WaypointRow(
                    waypoint = wp,
                    distance = if (!manualSort) userLocation?.let { formatDistance(distanceMeters(it, GeoPoint(wp.latitude, wp.longitude))) } else null,
                    showReorder = manualSort && searchQuery.isBlank(),
                    canMoveUp = manualSort && idx > 0,
                    canMoveDown = manualSort && idx < waypoints.size - 1,
                    onMoveUp = {
                        if (idx > 0) {
                            val list = waypoints.toMutableList()
                            list[idx] = list[idx - 1].also { list[idx - 1] = list[idx] }
                            onReorderWaypoints(list)
                        }
                    },
                    onMoveDown = {
                        if (idx < waypoints.size - 1) {
                            val list = waypoints.toMutableList()
                            list[idx] = list[idx + 1].also { list[idx + 1] = list[idx] }
                            onReorderWaypoints(list)
                        }
                    },
                    onClick = { onSelectWaypoint(wp) }
                )
            }

            // Tracks
            if (tracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${tracks.size} track${if (tracks.size != 1) "s" else ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track = track, onClick = { onSelectTrack(track) }, onDelete = { onDeleteTrack(track) })
                }
            }
        }
    }
}

@Composable
private fun WaypointRow(
    waypoint: Waypoint,
    distance: String?,
    showReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(waypoint.color)), CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(waypoint.name, fontWeight = FontWeight.W500, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            if (waypoint.notes.isNotBlank()) Text(waypoint.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
        if (distance != null) Text(distance, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        if (showReorder) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, "Move up", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, "Move down", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onDelete: () -> Unit) {
    val stats = remember(track) { computeTrackStats(track.points) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(track.color)), CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.name, fontWeight = FontWeight.W500, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("${formatDistance(stats.distanceMeters).replace(" away", "")}  \u00B7  ${formatDuration(stats.durationMs)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}
