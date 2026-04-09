package com.florian.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onDismiss: () -> Unit
) {
    val sorted = if (userLocation != null) {
        waypoints.sortedBy { distanceMeters(userLocation, GeoPoint(it.latitude, it.longitude)) }
    } else {
        waypoints.sortedBy { it.name }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Waypoints section
            item {
                Text(
                    "${waypoints.size} waypoint${if (waypoints.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (waypoints.isEmpty()) {
                item {
                    Text(
                        "No waypoints yet. Long-press the map to add one.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            items(sorted, key = { it.id }) { wp ->
                WaypointRow(
                    waypoint = wp,
                    distance = userLocation?.let {
                        formatDistance(distanceMeters(it, GeoPoint(wp.latitude, wp.longitude)))
                    },
                    onClick = { onSelectWaypoint(wp) }
                )
            }

            // Tracks section
            if (tracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${tracks.size} track${if (tracks.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onClick = { onSelectTrack(track) },
                        onDelete = { onDeleteTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaypointRow(waypoint: Waypoint, distance: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(android.graphics.Color.parseColor(waypoint.color)), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                waypoint.name,
                fontWeight = FontWeight.W500,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (waypoint.notes.isNotBlank()) {
                Text(waypoint.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
        }
        if (distance != null) {
            Text(distance, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(android.graphics.Color.parseColor(track.color)), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.name,
                fontWeight = FontWeight.W500,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val pts = track.points.size
            Text("$pts point${if (pts != 1) "s" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}
