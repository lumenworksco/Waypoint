package com.florian.waypoint

import android.Manifest
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private val SelectedPin = Color(0xFFDC5028)

@Composable
fun MapScreen(store: WaypointStore) {
    val context = LocalContext.current
    // ── Core state ──────────────────────────────────────────────
    var waypoints by remember { mutableStateOf(store.load()) }
    var tracks by remember { mutableStateOf(store.loadTracks()) }
    var selectedWaypoint by remember { mutableStateOf<Waypoint?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationEnabled by remember { mutableStateOf(false) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // ── UI state ────────────────────────────────────────────────
    var showEditDialog by remember { mutableStateOf(false) }
    var showWaypointList by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var mapStyle by remember { mutableStateOf(
        runCatching { MapStyle.valueOf(store.loadMapStyle()) }.getOrDefault(MapStyle.STANDARD)
    ) }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val trackRecorder = remember { TrackRecorder() }

    // ── Location ────────────────────────────────────────────────
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                userLocation = GeoPoint(loc.latitude, loc.longitude)
                locationEnabled = true
                if (trackRecorder.isRecording) {
                    trackRecorder.addPoint(loc.latitude, loc.longitude, loc.altitude)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            try {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                    .setMinUpdateDistanceMeters(2f)
                    .build()
                locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            } catch (_: SecurityException) { }
        }
    }

    // ── GPX SAF launchers ───────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        uri?.let {
            val gpx = exportGpx(waypoints, tracks)
            context.contentResolver.openOutputStream(it)?.use { os -> os.write(gpx.toByteArray()) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val xml = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@let
            val data = importGpx(xml)
            if (data.waypoints.isNotEmpty()) {
                waypoints = waypoints + data.waypoints
                store.save(waypoints)
            }
            if (data.tracks.isNotEmpty()) {
                tracks = tracks + data.tracks
                store.saveTracks(tracks)
            }
        }
    }

    // ── Init ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = "com.florian.waypoint"
            tileFileSystemCacheMaxBytes = 100L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 80L * 1024 * 1024
        }
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    DisposableEffect(Unit) {
        onDispose { locationClient.removeLocationUpdates(locationCallback) }
    }

    // ── Refresh all overlays ────────────────────────────────────
    fun refreshOverlays(mapView: MapView) {
        mapView.overlays.removeAll { it is Marker || it is Polyline }

        // Tracks (below markers)
        for (track in tracks) {
            mapView.overlays.add(Polyline(mapView).apply {
                setPoints(track.points.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.parseColor(track.color)
                outlinePaint.strokeWidth = 4f * context.resources.displayMetrics.density
                outlinePaint.isAntiAlias = true
            })
        }
        // Current recording
        if (trackRecorder.currentPoints.isNotEmpty()) {
            mapView.overlays.add(Polyline(mapView).apply {
                setPoints(trackRecorder.currentPoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.parseColor("#FF2D55")
                outlinePaint.strokeWidth = 5f * context.resources.displayMetrics.density
                outlinePaint.isAntiAlias = true
            })
        }

        // Waypoint markers
        for (wp in waypoints) {
            val isSelected = wp.id == selectedWaypoint?.id
            val pinColor = if (isSelected) SelectedPin
                else Color(android.graphics.Color.parseColor(wp.color))
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(wp.latitude, wp.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = createPinDrawable(context, pinColor, wp.name)
                title = wp.name
                id = wp.id
                setOnMarkerClickListener { m, _ ->
                    vibrate(context, light = true)
                    selectedWaypoint = waypoints.find { it.id == m.id }
                    refreshOverlays(mapView)
                    true
                }
            })
        }

        // User dot
        userLocation?.let { loc ->
            mapView.overlays.add(Marker(mapView).apply {
                position = loc
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createUserDotDrawable(context)
                setOnMarkerClickListener { _, _ -> true }
            })
        }
        mapView.invalidate()
    }

    // ── Effects ─────────────────────────────────────────────────
    LaunchedEffect(userLocation, trackRecorder.currentPoints.size) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        refreshOverlays(map)
        val loc = userLocation ?: return@LaunchedEffect
        if (!hasCenteredOnUser) {
            map.controller.animateTo(loc, 18.5, 600)
            hasCenteredOnUser = true
        }
    }

    LaunchedEffect(waypoints, selectedWaypoint, tracks) {
        mapViewRef.value?.let { refreshOverlays(it) }
    }

    LaunchedEffect(mapStyle) {
        mapViewRef.value?.let {
            it.setTileSource(mapStyle.tileSource())
            it.invalidate()
        }
    }

    // ── UI ──────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(mapStyle.tileSource())
                    setMultiTouchControls(true)
                    @Suppress("DEPRECATION")
                    setBuiltInZoomControls(false)
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(51.5074, -0.1278))

                    overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            selectedWaypoint = null
                            refreshOverlays(this@apply)
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
                            refreshOverlays(this@apply)
                            return true
                        }
                    }))

                    mapViewRef.value = this
                }
            }
        )

        // ── Top-left: coordinate header ─────────────────────────
        CoordinateHeader(
            userLocation = userLocation,
            locationEnabled = locationEnabled,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp)
                .align(Alignment.TopStart)
        )

        // ── Top-right: style + list + overflow buttons ──────────
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(end = 16.dp, top = 10.dp)
                .align(Alignment.TopEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Map style
            Box {
                SmallButton(Icons.Filled.Layers, "Map style") { showStyleMenu = true }
                DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                    MapStyle.entries.forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.label) },
                            onClick = {
                                mapStyle = style
                                store.saveMapStyle(style.name)
                                mapViewRef.value?.let {
                                    it.setTileSource(style.tileSource())
                                    it.invalidate()
                                }
                                showStyleMenu = false
                            }
                        )
                    }
                }
            }

            // Waypoint list
            SmallButton(Icons.AutoMirrored.Filled.List, "Waypoint list") { showWaypointList = true }

            // Overflow menu (GPX + offline)
            Box {
                SmallButton(Icons.Filled.MoreVert, "More") { showOverflowMenu = true }
                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Export GPX") },
                        onClick = {
                            showOverflowMenu = false
                            exportLauncher.launch("waypoints.gpx")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import GPX") },
                        onClick = {
                            showOverflowMenu = false
                            importLauncher.launch(arrayOf("*/*"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Download area") },
                        onClick = {
                            showOverflowMenu = false
                            val map = mapViewRef.value ?: return@DropdownMenuItem
                            val tileSource = map.tileProvider.tileSource
                            if (tileSource !is org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase) return@DropdownMenuItem
                            val bb = map.boundingBox
                            val zoom = map.zoomLevelDouble.toInt()
                            val maxZoom = minOf(zoom + 2, 19)
                            downloadProgress = 0.01f
                            val handler = Handler(Looper.getMainLooper())
                            Thread {
                                try {
                                    val cm = CacheManager(map)
                                    val tiles = CacheManager.getTilesCoverage(bb, zoom, maxZoom)
                                    val total = tiles.size.coerceAtLeast(1)
                                    var done = 0
                                    for (tile in tiles) {
                                        cm.loadTile(tileSource, tile)
                                        done++
                                        if (done % 5 == 0 || done == total) {
                                            handler.post { downloadProgress = done.toFloat() / total }
                                        }
                                    }
                                } catch (_: Exception) { }
                                handler.post { downloadProgress = null }
                            }.start()
                        }
                    )
                }
            }
        }

        // ── Download progress bar ───────────────────────────────
        downloadProgress?.let { progress ->
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .align(Alignment.Center),
            )
        }

        // ── Bottom row ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Card / hint
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

            // Right column: record + zoom + recenter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Record button
                RecordButton(isRecording = trackRecorder.isRecording) {
                    vibrate(context)
                    if (trackRecorder.isRecording) {
                        val track = trackRecorder.stop()
                        if (track.points.size >= 2) {
                            tracks = tracks + track
                            store.saveTracks(tracks)
                        }
                    } else {
                        trackRecorder.start()
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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

    // ── Dialogs / sheets ────────────────────────────────────────
    if (showEditDialog) {
        selectedWaypoint?.let { wp ->
            EditWaypointDialog(
                waypoint = wp,
                onDismiss = { showEditDialog = false },
                onSave = { name, notes, color ->
                    val sanitized = name.trim().take(64).ifBlank { "Unnamed Waypoint" }
                    waypoints = waypoints.map {
                        if (it.id == wp.id) it.copy(name = sanitized, notes = notes.trim(), color = color) else it
                    }
                    store.save(waypoints)
                    selectedWaypoint = waypoints.find { it.id == wp.id }
                    showEditDialog = false
                    vibrate(context, light = true)
                }
            )
        }
    }

    if (showWaypointList) {
        WaypointListSheet(
            waypoints = waypoints,
            tracks = tracks,
            userLocation = userLocation,
            onSelectWaypoint = { wp ->
                selectedWaypoint = wp
                mapViewRef.value?.controller?.animateTo(GeoPoint(wp.latitude, wp.longitude), 18.0, 800)
                showWaypointList = false
            },
            onSelectTrack = { track ->
                if (track.points.isNotEmpty()) {
                    val center = GeoPoint(
                        track.points.sumOf { it.latitude } / track.points.size,
                        track.points.sumOf { it.longitude } / track.points.size
                    )
                    mapViewRef.value?.controller?.animateTo(center, 15.0, 800)
                }
                showWaypointList = false
            },
            onDeleteTrack = { track ->
                tracks = tracks.filter { it.id != track.id }
                store.saveTracks(tracks)
            },
            onDismiss = { showWaypointList = false }
        )
    }
}

// ── Small map buttons ───────────────────────────────────────────
@Composable
private fun SmallButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 3.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ZoomButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RecenterButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (enabled) 4.dp else 1.dp,
        onClick = { if (enabled) onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Center on my location",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = if (isRecording) Color(0xFFFF2D55) else MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                Icon(Icons.Filled.Stop, "Stop recording", tint = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Filled.FiberManualRecord, "Record track", tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
            }
        }
    }
}
