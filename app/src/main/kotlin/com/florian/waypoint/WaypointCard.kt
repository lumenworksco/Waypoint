package com.florian.waypoint

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint
import java.io.File

val PresetColors = listOf("#3C3734", "#007AFF", "#34C759", "#FF9500", "#AF52DE", "#FF2D55")

// ── Coordinate header ───────────────────────────────────────────
@Composable
fun CoordinateHeader(
    userLocation: GeoPoint?,
    locationEnabled: Boolean,
    coordFormat: CoordFormat,
    onToggleFormat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        onClick = onToggleFormat
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
                text = if (userLocation != null) formatCoordinate(userLocation.latitude, userLocation.longitude, coordFormat)
                else "Locating\u2026",
                fontSize = 13.sp,
                fontWeight = FontWeight.W500,
                color = if (locationEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ── Hint capsule ────────────────────────────────────────────────
@Composable
fun HintCapsule() {
    Surface(shape = RoundedCornerShape(50.dp), color = Color.Black.copy(alpha = 0.65f)) {
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
    var showQrDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(waypoint.color)), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(waypoint.name, fontWeight = FontWeight.W600, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (waypoint.notes.isNotBlank()) Text(waypoint.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                    if (distance != null) Text(distance, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.width(8.dp))
                CardIconButton(Icons.Filled.Edit, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Edit", onEdit)
                Spacer(modifier = Modifier.width(6.dp))
                CardIconButton(Icons.Filled.Delete, Color(0xFFFF3B30), Color.White, "Delete", onDelete)
                Spacer(modifier = Modifier.width(6.dp))
                CardIconButton(Icons.Filled.Close, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Close", onClose)
            }
            // Photo thumbnail
            waypoint.photoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                    bmp?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            // Action row
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                TextButton(onClick = { shareWaypoint(context, waypoint) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Share", fontSize = 12.sp)
                }
                TextButton(onClick = { navigateToWaypoint(context, waypoint) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Navigate", fontSize = 12.sp)
                }
                TextButton(onClick = { showQrDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("QR", fontSize = 12.sp)
                }
            }
        }
    }

    if (showQrDialog) {
        val geoUri = "geo:${waypoint.latitude},${waypoint.longitude}?q=${waypoint.latitude},${waypoint.longitude}(${Uri.encode(waypoint.name)})"
        val qrBitmap = remember(waypoint.id) { generateQrBitmap(geoUri) }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("QR Code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scan to open location", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = { TextButton(onClick = { showQrDialog = false }) { Text("Done") } }
        )
    }
}

@Composable
fun CardIconButton(icon: ImageVector, bgColor: Color, iconColor: Color, desc: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = bgColor, onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = iconColor, modifier = Modifier.size(15.dp))
        }
    }
}

// ── Edit dialog with color picker, icon picker, photo ───────────
@Composable
fun EditWaypointDialog(
    waypoint: Waypoint,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String, color: String, photoPath: String?, icon: String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(waypoint.name) }
    var notes by remember { mutableStateOf(waypoint.notes) }
    var selectedColor by remember { mutableStateOf(waypoint.color) }
    var selectedIcon by remember { mutableStateOf(waypoint.icon) }
    var photoPath by remember { mutableStateOf(waypoint.photoPath) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val dir = File(context.filesDir, "photos").also { d -> d.mkdirs() }
            val dest = File(dir, "${waypoint.id}.jpg")
            context.contentResolver.openInputStream(it)?.use { inp -> dest.outputStream().use { out -> inp.copyTo(out) } }
            photoPath = dest.absolutePath
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Waypoint") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                // Color picker
                Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (hex in PresetColors) {
                        Box(
                            modifier = Modifier.size(32.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                .then(if (hex.equals(selectedColor, true)) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .clickable { selectedColor = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Icon picker
                Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WaypointPresetIcons) { preset ->
                        val isSel = preset.key == selectedIcon
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            onClick = { selectedIcon = if (selectedIcon == preset.key) null else preset.key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSel) {
                                    Box(modifier = Modifier.matchParentSize().border(2.dp, MaterialTheme.colorScheme.primary, CircleShape))
                                }
                                Icon(preset.icon, preset.label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Photo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { photoLauncher.launch("image/*") }) { Text("Choose photo") }
                    if (photoPath != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { photoPath = null }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                    }
                }
                photoPath?.let { path ->
                    val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                    bmp?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, notes, selectedColor, photoPath, selectedIcon) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun shareWaypoint(context: Context, wp: Waypoint) {
    val text = "${wp.name}: ${wp.latitude}, ${wp.longitude}\nhttps://www.google.com/maps?q=${wp.latitude},${wp.longitude}"
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share"))
}

private fun navigateToWaypoint(context: Context, wp: Waypoint) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${wp.latitude},${wp.longitude}?q=${wp.latitude},${wp.longitude}(${Uri.encode(wp.name)})")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    } catch (_: Exception) { }
}
