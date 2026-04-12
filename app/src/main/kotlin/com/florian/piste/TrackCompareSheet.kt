package com.florian.piste

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet for side-by-side comparison of two recorded tracks.
 * User picks two tracks from dropdowns; the sheet displays a row-by-row comparison
 * (distance, duration, vertical, runs, max/avg speed) with the winner of each
 * metric highlighted in the primary color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackCompareSheet(tracks: List<Track>, imperial: Boolean, onDismiss: () -> Unit) {
    var trackA by remember { mutableStateOf<Track?>(null) }
    var trackB by remember { mutableStateOf<Track?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Compare Tracks", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize().iosClickable(onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Two pickers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackPicker(label = "Track A", track = trackA, onPick = { trackA = it }, modifier = Modifier.weight(1f), tracks = tracks)
                TrackPicker(label = "Track B", track = trackB, onPick = { trackB = it }, modifier = Modifier.weight(1f), tracks = tracks)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val a = trackA; val b = trackB
            if (a != null && b != null) {
                val sa = remember(a) { computeTrackStats(a.points) }
                val sb = remember(b) { computeTrackStats(b.points) }

                CompareRow("Distance", formatDistance(sa.distanceMeters, imperial).replace(" away", ""), formatDistance(sb.distanceMeters, imperial).replace(" away", ""), sa.distanceMeters > sb.distanceMeters)
                CompareRow("Duration", formatDuration(sa.durationMs), formatDuration(sb.durationMs), sa.durationMs > sb.durationMs)
                CompareRow("Vertical", formatVertical(sa.verticalDescended ?: 0.0, imperial), formatVertical(sb.verticalDescended ?: 0.0, imperial), (sa.verticalDescended ?: 0.0) > (sb.verticalDescended ?: 0.0))
                CompareRow("Runs", sa.runCount.toString(), sb.runCount.toString(), sa.runCount > sb.runCount)
                CompareRow("Max Speed", formatTrackSpeed(sa.maxSpeedKmh), formatTrackSpeed(sb.maxSpeedKmh), sa.maxSpeedKmh > sb.maxSpeedKmh)
                CompareRow("Avg Speed", formatTrackSpeed(sa.avgSpeedKmh), formatTrackSpeed(sb.avgSpeedKmh), sa.avgSpeedKmh > sb.avgSpeedKmh)
            } else {
                Text(
                    "Pick two tracks above to compare",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrackPicker(
    label: String,
    track: Track?,
    onPick: (Track) -> Unit,
    tracks: List<Track>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.fillMaxSize().iosClickable { expanded = true }, contentAlignment = Alignment.Center) {
                if (track != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(safeParseColor(track.color)), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            track.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                } else {
                    Text("Choose…", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            tracks.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.name, fontSize = 14.sp) },
                    onClick = { onPick(t); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, valueA: String, valueB: String, aWins: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            valueA,
            fontSize = 15.sp,
            fontWeight = if (aWins) FontWeight.W700 else FontWeight.W500,
            color = if (aWins) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp).widthIn(min = 80.dp),
            textAlign = TextAlign.Center
        )
        Text(
            valueB,
            fontSize = 15.sp,
            fontWeight = if (!aWins) FontWeight.W700 else FontWeight.W500,
            color = if (!aWins) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
    }
}
