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
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.File

private val SelectedPin = Color(0xFFDC5028)

@Composable
fun MapScreen(store: WaypointStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var coordFormat by remember { mutableStateOf(
        runCatching { CoordFormat.valueOf(store.loadCoordFormat()) }.getOrDefault(CoordFormat.DECIMAL)
    ) }

    // Delete flow
    var pendingDelete by remember { mutableStateOf<Waypoint?>(null) }
    var lastDeleted by remember { mutableStateOf<Waypoint?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Measure mode
    var measureMode by remember { mutableStateOf(false) }
    var measureResult by remember { mutableStateOf<String?>(null) }
    val measureOverlayRef = remember { mutableStateOf<MeasureOverlay?>(null) }

    // Track detail
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

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
                if (trackRecorder.isRecording) trackRecorder.addPoint(loc.latitude, loc.longitude, loc.altitude)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            try {
                locationClient.requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).setMinUpdateDistanceMeters(2f).build(),
                    locationCallback, Looper.getMainLooper()
                )
            } catch (_: SecurityException) { }
        }
    }

    // ── GPX SAF launchers ───────────────────────────────────────
    val exportGpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(exportGpx(waypoints, tracks).toByteArray()) } }
    }
    val importGpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val xml = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@let
            val data = importGpx(xml)
            if (data.waypoints.isNotEmpty()) { waypoints = waypoints + data.waypoints; store.save(waypoints) }
            if (data.tracks.isNotEmpty()) { tracks = tracks + data.tracks; store.saveTracks(tracks) }
        }
    }

    // ── Backup SAF launchers ────────────────────────────────────
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> createBackupZip(context, os) } }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { ins ->
                if (restoreBackupZip(context, ins)) {
                    waypoints = store.load(); tracks = store.loadTracks()
                    mapStyle = runCatching { MapStyle.valueOf(store.loadMapStyle()) }.getOrDefault(MapStyle.STANDARD)
                    coordFormat = runCatching { CoordFormat.valueOf(store.loadCoordFormat()) }.getOrDefault(CoordFormat.DECIMAL)
                }
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
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    DisposableEffect(Unit) { onDispose { locationClient.removeLocationUpdates(locationCallback) } }

    // ── Refresh overlays ────────────────────────────────────────
    fun refreshOverlays(mapView: MapView) {
        mapView.overlays.removeAll { it is Marker || it is Polyline }
        val density = context.resources.displayMetrics.density

        // Tracks
        for (track in tracks) {
            mapView.overlays.add(Polyline(mapView).apply {
                setPoints(track.points.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.parseColor(track.color)
                outlinePaint.strokeWidth = 3f * density; outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND; outlinePaint.isAntiAlias = true
            })
        }
        if (trackRecorder.currentPoints.isNotEmpty()) {
            mapView.overlays.add(Polyline(mapView).apply {
                setPoints(trackRecorder.currentPoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.parseColor("#FF2D55")
                outlinePaint.strokeWidth = 3.5f * density; outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND; outlinePaint.isAntiAlias = true
            })
        }

        // Waypoint markers (with clustering)
        val clusters = clusterWaypoints(waypoints, mapView)
        for (cluster in clusters) {
            if (cluster.items.size == 1) {
                val wp = cluster.items.first()
                val isSelected = wp.id == selectedWaypoint?.id
                val pinColor = if (isSelected) SelectedPin else Color(android.graphics.Color.parseColor(wp.color))
                mapView.overlays.add(Marker(mapView).apply {
                    position = GeoPoint(wp.latitude, wp.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createPinDrawable(context, pinColor, wp.name, wp.icon)
                    id = wp.id; setInfoWindow(null)
                    setOnMarkerClickListener { m, _ ->
                        vibrate(context, light = true)
                        selectedWaypoint = waypoints.find { it.id == m.id }
                        refreshOverlays(mapView); true
                    }
                })
            } else {
                mapView.overlays.add(Marker(mapView).apply {
                    position = GeoPoint(cluster.centerLat, cluster.centerLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createClusterDrawable(context, cluster.items.size)
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, mv -> mv.controller.animateTo(position, mv.zoomLevelDouble + 2.0, 400); true }
                })
            }
        }

        // User dot
        userLocation?.let { loc ->
            mapView.overlays.add(Marker(mapView).apply {
                position = loc; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createUserDotDrawable(context); setInfoWindow(null)
                setOnMarkerClickListener { _, _ -> true }
            })
        }
        mapView.invalidate()
    }

    // ── Effects ─────────────────────────────────────────────────
    LaunchedEffect(userLocation, trackRecorder.currentPoints.size) {
        val map = mapViewRef.value ?: return@LaunchedEffect; refreshOverlays(map)
        val loc = userLocation ?: return@LaunchedEffect
        if (!hasCenteredOnUser) { map.controller.animateTo(loc, 18.5, 600); hasCenteredOnUser = true }
    }
    LaunchedEffect(waypoints, selectedWaypoint, tracks) { mapViewRef.value?.let { refreshOverlays(it) } }
    LaunchedEffect(mapStyle) { mapViewRef.value?.let { it.setTileSource(mapStyle.tileSource()); it.invalidate() } }

    // ── UI ──────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(mapStyle.tileSource()); setMultiTouchControls(true)
                @Suppress("DEPRECATION") setBuiltInZoomControls(false)
                minZoomLevel = 3.0; maxZoomLevel = 20.0
                controller.setZoom(15.0); controller.setCenter(GeoPoint(51.5074, -0.1278))

                overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        if (measureMode) return false
                        selectedWaypoint = null; refreshOverlays(this@apply); return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean {
                        vibrate(ctx)
                        val wp = Waypoint(name = "Waypoint ${waypoints.size + 1}", latitude = p.latitude, longitude = p.longitude)
                        waypoints = waypoints + wp; store.save(waypoints); refreshOverlays(this@apply); return true
                    }
                }))

                // Compass, scale bar, rotation
                overlays.add(CompassOverlay(ctx, this).apply { enableCompass() })
                overlays.add(ScaleBarOverlay(this).apply {
                    setCentred(false); setAlignBottom(true); setAlignRight(false)
                    setTextSize(10f * resources.displayMetrics.density)
                    setScaleBarOffset((16 * resources.displayMetrics.density).toInt(), (120 * resources.displayMetrics.density).toInt())
                })
                overlays.add(RotationGestureOverlay(this))

                // Refresh on zoom (for clustering)
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?) = false
                    override fun onZoom(event: ZoomEvent?) = false.also { refreshOverlays(this@apply) }
                })

                mapViewRef.value = this
            }
        })

        // Top-left: coordinate header
        CoordinateHeader(
            userLocation = userLocation, locationEnabled = locationEnabled,
            coordFormat = coordFormat,
            onToggleFormat = {
                coordFormat = CoordFormat.entries[(coordFormat.ordinal + 1) % CoordFormat.entries.size]
                store.saveCoordFormat(coordFormat.name)
            },
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp).align(Alignment.TopStart)
        )

        // Top-right: buttons
        Column(
            modifier = Modifier.statusBarsPadding().padding(end = 16.dp, top = 10.dp).align(Alignment.TopEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Map style
            Box {
                SmallButton(Icons.Filled.Layers, "Map style") { showStyleMenu = true }
                DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                    MapStyle.entries.forEach { style ->
                        DropdownMenuItem(text = { Text(style.label) }, onClick = {
                            mapStyle = style; store.saveMapStyle(style.name)
                            mapViewRef.value?.let { it.setTileSource(style.tileSource()); it.invalidate() }
                            showStyleMenu = false
                        })
                    }
                }
            }
            SmallButton(Icons.AutoMirrored.Filled.List, "Waypoints") { showWaypointList = true }
            // Reset north
            SmallButton(Icons.Filled.Explore, "Reset North") {
                vibrate(context, light = true)
                mapViewRef.value?.apply { mapOrientation = 0f; invalidate() }
            }
            // Overflow
            Box {
                SmallButton(Icons.Filled.MoreVert, "More") { showOverflowMenu = true }
                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                    DropdownMenuItem(text = { Text("Export GPX") }, onClick = { showOverflowMenu = false; exportGpxLauncher.launch("waypoints.gpx") })
                    DropdownMenuItem(text = { Text("Import GPX") }, onClick = { showOverflowMenu = false; importGpxLauncher.launch(arrayOf("*/*")) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Backup all data") }, onClick = { showOverflowMenu = false; backupExportLauncher.launch("waypoint-backup.zip") })
                    DropdownMenuItem(text = { Text("Restore from backup") }, onClick = { showOverflowMenu = false; backupImportLauncher.launch(arrayOf("*/*")) })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (measureMode) "Stop measuring" else "Measure distance") },
                        onClick = {
                            showOverflowMenu = false; measureMode = !measureMode
                            if (measureMode) {
                                mapViewRef.value?.let { mv ->
                                    val ov = MeasureOverlay { measureResult = it }
                                    ov.activate(); measureOverlayRef.value = ov; mv.overlays.add(ov); mv.invalidate()
                                }
                            } else {
                                measureOverlayRef.value?.let { mapViewRef.value?.overlays?.remove(it); mapViewRef.value?.invalidate() }
                                measureOverlayRef.value = null; measureResult = null
                            }
                        }
                    )
                    DropdownMenuItem(text = { Text("Download area") }, onClick = {
                        showOverflowMenu = false
                        val map = mapViewRef.value ?: return@DropdownMenuItem
                        val ts = map.tileProvider.tileSource
                        if (ts !is OnlineTileSourceBase) return@DropdownMenuItem
                        val bb = map.boundingBox; val zoom = map.zoomLevelDouble.toInt(); val maxZ = minOf(zoom + 2, 19)
                        downloadProgress = 0.01f
                        val handler = Handler(Looper.getMainLooper())
                        Thread {
                            try {
                                val cm = CacheManager(map); val tiles = CacheManager.getTilesCoverage(bb, zoom, maxZ)
                                val total = tiles.size.coerceAtLeast(1); var done = 0
                                for (tile in tiles) { cm.loadTile(ts, tile); done++; if (done % 5 == 0 || done == total) handler.post { downloadProgress = done.toFloat() / total } }
                            } catch (_: Exception) { }
                            handler.post { downloadProgress = null }
                        }.start()
                    })
                }
            }
        }

        // Measure result banner
        measureResult?.let { result ->
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp),
                shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shadowElevation = 4.dp
            ) { Text(result, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp) }
        }

        // Download progress
        downloadProgress?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).align(Alignment.Center))
        }

        // Bottom row
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(modifier = Modifier.weight(1f)) {
                val sel = selectedWaypoint
                if (sel != null) {
                    WaypointCard(
                        waypoint = sel,
                        distance = userLocation?.let { formatDistance(distanceMeters(it, GeoPoint(sel.latitude, sel.longitude))) },
                        onEdit = { showEditDialog = true },
                        onDelete = { pendingDelete = sel },
                        onClose = { selectedWaypoint = null }
                    )
                } else if (waypoints.isEmpty()) HintCapsule()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Pause/resume when recording
                if (trackRecorder.isRecording) {
                    SmallButton(
                        icon = if (trackRecorder.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        description = if (trackRecorder.isPaused) "Resume" else "Pause"
                    ) { vibrate(context, light = true); if (trackRecorder.isPaused) trackRecorder.resume() else trackRecorder.pause() }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                RecordButton(isRecording = trackRecorder.isRecording, isPaused = trackRecorder.isPaused) {
                    vibrate(context)
                    if (trackRecorder.isRecording) {
                        val track = trackRecorder.stop()
                        if (track.points.size >= 2) { tracks = tracks + track; store.saveTracks(tracks) }
                    } else trackRecorder.start()
                }
                Spacer(modifier = Modifier.height(12.dp))
                ZoomButton(Icons.Filled.Add, "Zoom in") { vibrate(context, light = true); mapViewRef.value?.controller?.zoomIn() }
                Spacer(modifier = Modifier.height(8.dp))
                ZoomButton(Icons.Filled.Remove, "Zoom out") { vibrate(context, light = true); mapViewRef.value?.controller?.zoomOut() }
                Spacer(modifier = Modifier.height(12.dp))
                RecenterButton(enabled = locationEnabled) {
                    vibrate(context, light = true)
                    userLocation?.let { mapViewRef.value?.controller?.animateTo(it, 18.5, 600) }
                }
            }
        }

        // Snackbar host
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp))
    }

    // ── Dialogs / sheets ────────────────────────────────────────
    // Confirm delete
    pendingDelete?.let { wp ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Waypoint") },
            text = { Text("Delete \"${wp.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    // Delete + photo cleanup
                    wp.photoPath?.let { File(it).delete() }
                    waypoints = waypoints.filter { it.id != wp.id }; store.save(waypoints)
                    selectedWaypoint = null; lastDeleted = wp; pendingDelete = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar("Deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed && lastDeleted != null) {
                            waypoints = waypoints + lastDeleted!!; store.save(waypoints); lastDeleted = null
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    // Edit dialog
    if (showEditDialog) {
        selectedWaypoint?.let { wp ->
            EditWaypointDialog(
                waypoint = wp,
                onDismiss = { showEditDialog = false },
                onSave = { name, notes, color, photoPath, icon ->
                    val sanitized = name.trim().take(64).ifBlank { "Unnamed Waypoint" }
                    waypoints = waypoints.map { if (it.id == wp.id) it.copy(name = sanitized, notes = notes.trim(), color = color, photoPath = photoPath, icon = icon) else it }
                    store.save(waypoints); selectedWaypoint = waypoints.find { it.id == wp.id }
                    showEditDialog = false; vibrate(context, light = true)
                }
            )
        }
    }

    // Waypoint list
    if (showWaypointList) {
        WaypointListSheet(
            waypoints = waypoints, tracks = tracks, userLocation = userLocation,
            onSelectWaypoint = { wp ->
                selectedWaypoint = wp; mapViewRef.value?.controller?.animateTo(GeoPoint(wp.latitude, wp.longitude), 18.0, 800)
                showWaypointList = false
            },
            onSelectTrack = { track ->
                selectedTrack = track
                if (track.points.isNotEmpty()) {
                    val c = GeoPoint(track.points.sumOf { it.latitude } / track.points.size, track.points.sumOf { it.longitude } / track.points.size)
                    mapViewRef.value?.controller?.animateTo(c, 15.0, 800)
                }
                showWaypointList = false
            },
            onDeleteTrack = { track -> tracks = tracks.filter { it.id != track.id }; store.saveTracks(tracks) },
            onReorderWaypoints = { reordered -> waypoints = reordered; store.save(waypoints); store.saveWaypointOrder(reordered.map { it.id }) },
            onDismiss = { showWaypointList = false }
        )
    }

    // Track detail
    selectedTrack?.let { track ->
        TrackDetailSheet(track = track, onDelete = {
            tracks = tracks.filter { it.id != track.id }; store.saveTracks(tracks); selectedTrack = null
        }, onDismiss = { selectedTrack = null })
    }
}

// ── Small buttons ───────────────────────────────────────────────
@Composable
private fun SmallButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shadowElevation = 3.dp, onClick = onClick) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun ZoomButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp, onClick = onClick) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun RecenterButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = if (enabled) 4.dp else 1.dp, onClick = { if (enabled) onClick() }) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.MyLocation, "Center", tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, isPaused: Boolean = false, onClick: () -> Unit) {
    val bg = when { isRecording && isPaused -> Color(0xFFFF2D55).copy(alpha = 0.5f); isRecording -> Color(0xFFFF2D55); else -> MaterialTheme.colorScheme.surface }
    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = bg, shadowElevation = 4.dp, onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) Icon(Icons.Filled.Stop, "Stop", tint = Color.White, modifier = Modifier.size(22.dp))
            else Icon(Icons.Filled.FiberManualRecord, "Record", tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
        }
    }
}
