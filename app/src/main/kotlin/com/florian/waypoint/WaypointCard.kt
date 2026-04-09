package com.florian.waypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint

val PresetColors = listOf("#3C3734", "#007AFF", "#34C759", "#FF9500", "#AF52DE", "#FF2D55")

// ── Coordinate header ───────────────────────────────────────────
@Composable
fun CoordinateHeader(
    userLocation: GeoPoint?,
    locationEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        if (locationEnabled) MaterialTheme.colorScheme.primary else Color(0xFFAEAEB2),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = if (userLocation != null)
                    "%.4f,  %.4f".format(userLocation.latitude, userLocation.longitude)
                else "Locating\u2026",
                fontSize = 13.sp,
                fontWeight = FontWeight.W500,
                color = if (locationEnabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outline,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ── Hint capsule ────────────────────────────────────────────────
@Composable
fun HintCapsule() {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color.Black.copy(alpha = 0.65f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("Long-press to add", color = Color.White, fontSize = 13.sp)
        }
    }
}

// ── Waypoint detail card ────────────────────────────────────────
@Composable
fun WaypointCard(
    waypoint: Waypoint,
    distance: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(android.graphics.Color.parseColor(waypoint.color)), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        waypoint.name,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (waypoint.notes.isNotBlank()) {
                        Text(waypoint.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                    }
                    if (distance != null) {
                        Text(distance, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                CardIconButton(Icons.Filled.Edit, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Edit", onEdit)
                Spacer(modifier = Modifier.width(6.dp))
                CardIconButton(Icons.Filled.Delete, Color(0xFFFF3B30), Color.White, "Delete", onDelete)
                Spacer(modifier = Modifier.width(6.dp))
                CardIconButton(Icons.Filled.Close, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Close", onClose)
            }
            // Share / Navigate row
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                TextButton(onClick = { shareWaypoint(context, waypoint) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Share", fontSize = 12.sp)
                }
                TextButton(onClick = { navigateToWaypoint(context, waypoint) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Navigate", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CardIconButton(
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = bgColor,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = iconColor, modifier = Modifier.size(15.dp))
        }
    }
}

// ── Edit dialog with color picker ───────────────────────────────
@Composable
fun EditWaypointDialog(
    waypoint: Waypoint,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(waypoint.name) }
    var notes by remember { mutableStateOf(waypoint.notes) }
    var selectedColor by remember { mutableStateOf(waypoint.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Waypoint") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (hex in PresetColors) {
                        val isSelected = hex.equals(selectedColor, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, notes, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Share / Navigate helpers ────────────────────────────────────
private fun shareWaypoint(context: Context, wp: Waypoint) {
    val text = "${wp.name}: ${wp.latitude}, ${wp.longitude}\nhttps://www.google.com/maps?q=${wp.latitude},${wp.longitude}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share waypoint"))
}

private fun navigateToWaypoint(context: Context, wp: Waypoint) {
    val geoUri = Uri.parse("geo:${wp.latitude},${wp.longitude}?q=${wp.latitude},${wp.longitude}(${Uri.encode(wp.name)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (_: Exception) { }
}
