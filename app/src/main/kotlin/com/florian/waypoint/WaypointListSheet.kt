package com.florian.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint

/**
 * Bottom sheet that lists all waypoints (sorted by distance from the user) and recorded
 * tracks, with a search field at the top. Tapping a waypoint flies the map to it and
 * selects it; tapping a track opens the track detail sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointListSheet(
    waypoints: List<Waypoint>,
    tracks: List<Track>,
    userLocation: GeoPoint?,
    onSelectWaypoint: (Waypoint) -> Unit,
    onSelectTrack: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val sorted = if (userLocation != null) {
        waypoints.sortedBy { distanceMeters(userLocation, GeoPoint(it.latitude, it.longitude)) }
    } else {
        waypoints.sortedBy { it.name }
    }

    val filtered = if (searchQuery.isBlank()) sorted else sorted.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        // Search bar
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search", color = MaterialTheme.colorScheme.outline, fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(Icons.Filled.Clear, "Clear", tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp).iosClickable { searchQuery = "" })
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    "${filtered.size} waypoint${if (filtered.size != 1) "s" else ""}",
                    fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                item { Text("No matches", color = MaterialTheme.colorScheme.outline, fontSize = 15.sp, modifier = Modifier.padding(vertical = 16.dp)) }
            }

            items(filtered, key = { it.id }) { wp ->
                WaypointRow(
                    waypoint = wp,
                    distance = userLocation?.let { formatDistance(distanceMeters(it, GeoPoint(wp.latitude, wp.longitude))) },
                    onClick = { onSelectWaypoint(wp) }
                )
            }

            if (tracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${tracks.size} track${if (tracks.size != 1) "s" else ""}", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 6.dp))
                }
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track = track, onClick = { onSelectTrack(track) }, onDelete = { onDeleteTrack(track) })
                }
            }
        }
    }
}

@Composable
private fun WaypointRow(waypoint: Waypoint, distance: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().iosClickable(onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(safeParseColor(waypoint.color)), CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(waypoint.name, fontWeight = FontWeight.W500, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            if (waypoint.notes.isNotBlank()) Text(waypoint.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
        if (distance != null) Text(distance, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onDelete: () -> Unit) {
    val stats = remember(track) { computeTrackStats(track.points) }
    Row(
        modifier = Modifier.fillMaxWidth().iosClickable(onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(safeParseColor(track.color)), CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.name, fontWeight = FontWeight.W500, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("${formatDistance(stats.distanceMeters).replace(" away", "")}  \u00B7  ${formatDuration(stats.durationMs)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
        Text("Delete", fontSize = 13.sp, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.iosClickable(onDelete).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
