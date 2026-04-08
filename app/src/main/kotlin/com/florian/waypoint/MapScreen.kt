package com.florian.waypoint

import android.Manifest
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.*

// ── Colors ──────────────────────────────────────────────────────
private val PrimaryBlue = Color(0xFF007AFF)
private val Charcoal = Color(0xFF3C3734)
private val SelectedPin = Color(0xFFDC5028)
private val DeleteRed = Color(0xFFFF3B30)
private val TextPrimary = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBackground = Color(0xFFF2F2F7)

@Composable
fun MapScreen(store: WaypointStore) {
    val context = LocalContext.current
    var waypoints by remember { mutableStateOf(store.load()) }
    var selectedWaypoint by remember { mutableStateOf<Waypoint?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationEnabled by remember { mutableStateOf(false) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // ── Location setup ──────────────────────────────────────────
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                userLocation = GeoPoint(loc.latitude, loc.longitude)
                locationEnabled = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            try {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                    .setMinUpdateDistanceMeters(2f)
                    .build()
                locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            } catch (_: SecurityException) { }
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "com.florian.waypoint"
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose { locationClient.removeLocationUpdates(locationCallback) }
    }

    // ── Helper: refresh markers on the map ──────────────────────
    fun refreshMarkers(mapView: MapView) {
        mapView.overlays.removeAll { it is Marker }
        for (wp in waypoints) {
            val isSelected = wp.id == selectedWaypoint?.id
            val marker = Marker(mapView).apply {
                position = GeoPoint(wp.latitude, wp.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = createPinDrawable(context, if (isSelected) SelectedPin else Charcoal, wp.name)
                title = wp.name
                id = wp.id
                setOnMarkerClickListener { m, _ ->
                    vibrate(context, light = true)
                    selectedWaypoint = waypoints.find { it.id == m.id }
                    refreshMarkers(mapView)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        // User location dot
        userLocation?.let { loc ->
            val dot = Marker(mapView).apply {
                position = loc
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createUserDotDrawable(context)
                setOnMarkerClickListener { _, _ -> true }
            }
            mapView.overlays.add(dot)
        }
        mapView.invalidate()
    }

    // ── Auto-center on first location fix ───────────────────────
    LaunchedEffect(userLocation) {
        val loc = userLocation ?: return@LaunchedEffect
        val map = mapViewRef.value ?: return@LaunchedEffect
        refreshMarkers(map)
        if (!hasCenteredOnUser) {
            map.controller.animateTo(loc, 18.5, 600)
            hasCenteredOnUser = true
        }
    }

    // ── Refresh markers when waypoints or selection changes ─────
    LaunchedEffect(waypoints, selectedWaypoint) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        refreshMarkers(map)
    }

    // ── UI ──────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(51.5074, -0.1278))

                    // Long-press to add waypoint
                    overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            selectedWaypoint = null
                            refreshMarkers(this@apply)
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean {
                            vibrate(ctx)
                            val wp = Waypoint(
                                name = "Waypoint ${waypoints.size + 1}",
                                latitude = p.latitude,
                                longitude = p.longitude
                            )
                            waypoints = waypoints + wp
                            store.save(waypoints)
                            refreshMarkers(this@apply)
                            return true
                        }
                    }))

                    mapViewRef.value = this
                }
            }
        )

        // ── Coordinate header (top-left) ────────────────────────
        CoordinateHeader(
            userLocation = userLocation,
            locationEnabled = locationEnabled,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp)
                .align(Alignment.TopStart)
        )

        // ── Bottom row ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Waypoint card or hint
            Box(modifier = Modifier.weight(1f)) {
                val sel = selectedWaypoint
                if (sel != null) {
                    WaypointCard(
                        waypoint = sel,
                        distance = userLocation?.let { formatDistance(distanceMeters(it, GeoPoint(sel.latitude, sel.longitude))) },
                        onEdit = { showEditDialog = true },
                        onDelete = {
                            vibrate(context)
                            waypoints = waypoints.filter { it.id != sel.id }
                            store.save(waypoints)
                            selectedWaypoint = null
                        },
                        onClose = { selectedWaypoint = null }
                    )
                } else if (waypoints.isEmpty()) {
                    HintCapsule()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Zoom + recenter column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ZoomButton(icon = Icons.Filled.Add, description = "Zoom in") {
                    vibrate(context, light = true)
                    mapViewRef.value?.controller?.zoomIn()
                }
                Spacer(modifier = Modifier.height(8.dp))
                ZoomButton(icon = Icons.Filled.Remove, description = "Zoom out") {
                    vibrate(context, light = true)
                    mapViewRef.value?.controller?.zoomOut()
                }
                Spacer(modifier = Modifier.height(12.dp))
                RecenterButton(enabled = locationEnabled) {
                    vibrate(context, light = true)
                    userLocation?.let { loc ->
                        mapViewRef.value?.controller?.animateTo(loc, 18.5, 600)
                    }
                }
            }
        }
    }

    // ── Edit dialog ─────────────────────────────────────────────
    if (showEditDialog) {
        selectedWaypoint?.let { wp ->
            EditWaypointDialog(
                waypoint = wp,
                onDismiss = { showEditDialog = false },
                onSave = { name, notes ->
                    val sanitized = name.trim().take(64).ifBlank { "Unnamed Waypoint" }
                    waypoints = waypoints.map {
                        if (it.id == wp.id) it.copy(name = sanitized, notes = notes.trim()) else it
                    }
                    store.save(waypoints)
                    selectedWaypoint = waypoints.find { it.id == wp.id }
                    showEditDialog = false
                    vibrate(context, light = true)
                }
            )
        }
    }
}

// ── Coordinate header ───────────────────────────────────────────
@Composable
private fun CoordinateHeader(
    userLocation: GeoPoint?,
    locationEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.92f),
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
                        if (locationEnabled) PrimaryBlue else Color(0xFFAEAEB2),
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
                color = if (locationEnabled) TextPrimary else TextSecondary,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ── Recenter button ─────────────────────────────────────────────
@Composable
private fun RecenterButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = if (enabled) 4.dp else 1.dp,
        onClick = { if (enabled) onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Center on my location",
                tint = PrimaryBlue.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Zoom button ─────────────────────────────────────────────────
@Composable
private fun ZoomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 3.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color(0xFF3C3C43),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Hint capsule ────────────────────────────────────────────────
@Composable
private fun HintCapsule() {
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
private fun WaypointCard(
    waypoint: Waypoint,
    distance: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    waypoint.name,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                if (waypoint.notes.isNotBlank()) {
                    Text(
                        waypoint.notes,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                if (distance != null) {
                    Text(distance, fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            CardIconButton(Icons.Filled.Edit, Color(0xFFEFEFF4), Color(0xFF3C3C43), "Edit", onEdit)
            Spacer(modifier = Modifier.width(6.dp))
            CardIconButton(Icons.Filled.Delete, DeleteRed, Color.White, "Delete", onDelete)
            Spacer(modifier = Modifier.width(6.dp))
            CardIconButton(Icons.Filled.Close, Color(0xFFEFEFF4), Color(0xFF3C3C43), "Close", onClose)
        }
    }
}

@Composable
private fun CardIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// ── Edit dialog ─────────────────────────────────────────────────
@Composable
private fun EditWaypointDialog(
    waypoint: Waypoint,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(waypoint.name) }
    var notes by remember { mutableStateOf(waypoint.notes) }

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, notes) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Drawing helpers ─────────────────────────────────────────────
private fun createPinDrawable(context: Context, color: Color, label: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density

    // Pin dimensions
    val headR = 13 * density      // head circle radius
    val pinH = 38 * density       // total pin height (head top to tip)
    val innerR = 5 * density      // white inner dot radius
    val labelGap = 3 * density

    // Measure label
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(230, 28, 28, 30)
        textSize = 10 * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val truncated = if (label.length > 12) label.take(11) + "\u2026" else label
    val textW = textPaint.measureText(truncated)
    val labelPadH = 6 * density
    val labelPadV = 3 * density
    val labelW = textW + labelPadH * 2
    val labelH = textPaint.textSize + labelPadV * 2

    // Canvas sizing
    val totalW = maxOf(labelW, headR * 2) + 6 * density
    val totalH = labelH + labelGap + pinH + 2 * density
    val bmp = Bitmap.createBitmap(totalW.toInt(), totalH.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = totalW / 2f

    // ── Label pill ──
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(245, 255, 255, 255)
        style = Paint.Style.FILL
        setShadowLayer(2 * density, 0f, 0.5f * density, android.graphics.Color.argb(40, 0, 0, 0))
    }
    canvas.drawRoundRect(
        cx - labelW / 2, 0f, cx + labelW / 2, labelH,
        6 * density, 6 * density, bgPaint
    )
    canvas.drawText(truncated, cx, labelPadV + textPaint.textSize - textPaint.descent(), textPaint)

    // ── Pin shape: arc over the top + two lines to the tip ──
    val pinTop = labelH + labelGap
    val headCy = pinTop + headR                 // circle center Y
    val tipY = pinTop + pinH                    // bottom tip

    val argb = android.graphics.Color.argb(
        (color.alpha * 255).toInt(), (color.red * 255).toInt(),
        (color.green * 255).toInt(), (color.blue * 255).toInt()
    )
    // Drop shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = argb
        style = Paint.Style.FILL
        setShadowLayer(3 * density, 0f, 1.5f * density, android.graphics.Color.argb(60, 0, 0, 0))
    }

    // Classic pin: wide arc (250°) + straight lines to tip
    // Arc starts at 145° (lower-left of circle) and sweeps 250° clockwise
    // ending at 395° = 35° (lower-right). Lines connect those endpoints to the tip.
    val path = Path().apply {
        arcTo(
            cx - headR, headCy - headR, cx + headR, headCy + headR,
            145f, 250f, true
        )
        lineTo(cx, tipY)
        close()
    }
    canvas.drawPath(path, shadowPaint)

    // White inner dot
    canvas.drawCircle(cx, headCy, innerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
    })

    return BitmapDrawable(context.resources, bmp)
}

private fun createUserDotDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f

    // Outer glow
    canvas.drawCircle(cx, cx, 18 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(36, 0, 122, 255)
    })
    // White ring
    canvas.drawCircle(cx, cx, 11 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })
    // Blue center
    canvas.drawCircle(cx, cx, 8 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 0, 122, 255)
    })

    return BitmapDrawable(context.resources, bmp)
}

// ── Distance ────────────────────────────────────────────────────
private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m away"
    else "%.1f km away".format(meters / 1000)

// ── Haptic ──────────────────────────────────────────────────────
private fun vibrate(context: Context, light: Boolean = false) {
    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    val effect = if (light)
        VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
    else
        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
    vibrator.vibrate(effect)
}
