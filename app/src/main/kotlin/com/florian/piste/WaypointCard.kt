package com.florian.piste

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

val PresetColors = listOf("#3C3734", "#007AFF", "#34C759", "#FF9500", "#AF52DE", "#FF2D55")

// ── iOS-style text field ────────────────────────────────────────
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline, fontSize = 15.sp) },
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
    )
}

/**
 * Simplified map header showing place name, altitude, and speed.
 * Long-press opens the speedometer sheet.
 */
@Composable
fun LocationHeader(
    placeName: String?,
    locationEnabled: Boolean,
    altitudeText: String?,
    speedText: String?,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .iosGlass()
    ) {
        Column(
            modifier = Modifier
                .iosClickable(onLongPress)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (locationEnabled) MaterialTheme.colorScheme.primary else Color(0xFFAEAEB2),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = placeName ?: if (locationEnabled) "Locating\u2026" else "Location off",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    color = if (locationEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    letterSpacing = 0.2.sp
                )
                if (speedText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(speedText, fontSize = 13.sp, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (altitudeText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    altitudeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 15.dp)
                )
            }
        }
    }
}

// ── Hint capsule ────────────────────────────────────────────────
@Composable
fun HintCapsule() {
    Surface(shape = RoundedCornerShape(50.dp), color = Color.Black.copy(alpha = 0.6f)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Long-press to add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W500)
        }
    }
}

/**
 * Glass card shown at the bottom of the map when a waypoint is selected.
 * Displays the waypoint's color dot, name, notes, distance from the user,
 * optional photo thumbnail, and action buttons (Edit, Delete, Close, Share,
 * Navigate, Set/Unset Home).
 */
@Composable
fun WaypointCard(
    waypoint: Waypoint,
    distance: String?,
    isHome: Boolean,
    onToggleHome: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp,
        modifier = Modifier.iosGlass(cornerRadius = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color(safeParseColor(waypoint.color)), CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(waypoint.name, fontWeight = FontWeight.W600, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (waypoint.notes.isNotBlank()) Text(waypoint.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                    if (distance != null) Text(distance, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IosIconButton(Icons.Filled.Edit, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Edit") { Haptics.tap(context); onEdit() }
                Spacer(modifier = Modifier.width(8.dp))
                IosIconButton(Icons.Filled.Delete, MaterialTheme.colorScheme.error.copy(alpha = 0.12f), MaterialTheme.colorScheme.error, "Delete") { Haptics.tap(context); onDelete() }
                Spacer(modifier = Modifier.width(8.dp))
                IosIconButton(Icons.Filled.Close, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Close") { Haptics.tap(context); onClose() }
            }
            // Photo thumbnail
            waypoint.photoPath?.let { path ->
                if (File(path).exists()) {
                    val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                    bmp?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Image(bitmap = it.asImageBitmap(), contentDescription = "Photo",
                            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop)
                    }
                }
            }
            // Action row
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IosTextAction("Share") { Haptics.tap(context); shareWaypoint(context, waypoint) }
                IosTextAction("Navigate") { Haptics.tap(context); navigateToWaypoint(context, waypoint) }
                IosTextAction(if (isHome) "Unset Home" else "Set Home") { Haptics.tap(context); onToggleHome() }
            }
        }
    }
}

@Composable
private fun IosTextAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.W500,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.iosClickable(onClick).padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
fun IosIconButton(icon: ImageVector, bgColor: Color, iconColor: Color, desc: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = bgColor
    ) {
        Box(modifier = Modifier.fillMaxSize().iosClickable(onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = iconColor, modifier = Modifier.size(16.dp))
        }
    }
}


// ── Drag handle for sheets ──────────────────────────────────────
@Composable
fun DragHandle() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.width(36.dp).height(5.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)))
    }
}

// ── Edit waypoint bottom sheet ──────────────────────────────────
/**
 * Bottom sheet for editing a waypoint's name, notes, color (6 presets),
 * ski-themed icon (12 presets), and an optional photo from the gallery.
 * Uses an iOS-style nav bar layout (Cancel / title / Save).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWaypointSheet(
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

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp,
                    modifier = Modifier.iosClickable(onDismiss))
                Spacer(modifier = Modifier.weight(1f))
                Text("Edit Waypoint", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Save",
                    color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.W600, fontSize = 15.sp,
                    modifier = Modifier.iosClickable { if (name.isNotBlank()) onSave(name, notes, selectedColor, photoPath, selectedIcon) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            IosTextField(value = name, onValueChange = { name = it.take(50) }, placeholder = "Name")
            Spacer(modifier = Modifier.height(10.dp))
            IosTextField(value = notes, onValueChange = { notes = it.take(500) }, placeholder = "Notes", singleLine = false, maxLines = 3)

            // Color picker
            Spacer(modifier = Modifier.height(18.dp))
            Text("Color", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (hex in PresetColors) {
                    Box(
                        modifier = Modifier.size(34.dp)
                            .background(Color(safeParseColor(hex)), CircleShape)
                            .then(if (hex.equals(selectedColor, true)) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .iosClickable { selectedColor = hex }
                    )
                }
            }

            // Icon picker
            Spacer(modifier = Modifier.height(18.dp))
            Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WaypointPresetIcons) { preset ->
                    val isSel = preset.key == selectedIcon
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .then(if (isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .iosClickable { selectedIcon = if (selectedIcon == preset.key) null else preset.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(preset.icon, preset.label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Photo
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose Photo", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp,
                    modifier = Modifier.iosClickable { photoLauncher.launch("image/*") })
                if (photoPath != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 15.sp,
                        modifier = Modifier.iosClickable { photoPath = null })
                }
            }
            photoPath?.let { path ->
                val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                bmp?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(bitmap = it.asImageBitmap(), contentDescription = "Photo",
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

// ── Delete confirmation bottom sheet ────────────────────────────
/**
 * iOS-style confirm sheet with a full-width red "Delete" button and a gray
 * "Cancel" button stacked vertically. Used to confirm waypoint deletion
 * before the undo-toast path runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmSheet(waypointName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Delete \"$waypointName\"?", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text("This action cannot be undone.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(20.dp))

            // Delete button (full-width, red)
            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error
            ) {
                Box(modifier = Modifier.fillMaxSize().iosClickable(onConfirm), contentAlignment = Alignment.Center) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.W600, fontSize = 17.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Cancel button (full-width, gray)
            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.fillMaxSize().iosClickable(onDismiss), contentAlignment = Alignment.Center) {
                    Text("Cancel", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
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
