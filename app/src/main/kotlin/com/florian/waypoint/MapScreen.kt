package com.florian.waypoint

import android.Manifest
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontWeight
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
    var showEditSheet by remember { mutableStateOf(false) }
    var showWaypointList by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var mapStyle by remember { mutableStateOf(runCatching { MapStyle.valueOf(store.loadMapStyle()) }.getOrDefault(MapStyle.STANDARD)) }
    var coordFormat by remember { mutableStateOf(runCatching { CoordFormat.valueOf(store.loadCoordFormat()) }.getOrDefault(CoordFormat.DECIMAL)) }

    // Delete flow
    var pendingDelete by remember { mutableStateOf<Waypoint?>(null) }
    var lastDeleted by remember { mutableStateOf<Waypoint?>(null) }
    var showUndoToast by remember { mutableStateOf(false) }

    // Measure mode
    var measureMode by remember { mutableStateOf(false) }
    var measureResult by remember { mutableStateOf<String?>(null) }
    val measureOverlayRef = remember { mutableStateOf<MeasureOverlay?>(null) }

    // Track detail
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var mapRotation by remember { mutableFloatStateOf(0f) }
    var metersPerPx by remember { mutableFloatStateOf(1f) }
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) {
            try {
                locationClient.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).setMinUpdateDistanceMeters(2f).build(), locationCallback, Looper.getMainLooper())
            } catch (_: SecurityException) { }
        }
    }

    // ── SAF launchers ───────────────────────────────────────────
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
        Configuration.getInstance().apply { userAgentValue = "com.florian.waypoint"; tileFileSystemCacheMaxBytes = 100L * 1024 * 1024; tileFileSystemCacheTrimBytes = 80L * 1024 * 1024 }
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
                outlinePaint.color = android.graphics.Color.parseColor("#FF2D55"); outlinePaint.strokeWidth = 3.5f * density
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND; outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND; outlinePaint.isAntiAlias = true
            })
        }

        // Clustered waypoint markers
        val clusters = clusterWaypoints(waypoints, mapView)
        for (cluster in clusters) {
            if (cluster.items.size == 1) {
                val wp = cluster.items.first(); val isSelected = wp.id == selectedWaypoint?.id
                val pinColor = if (isSelected) SelectedPin else Color(android.graphics.Color.parseColor(wp.color))
                mapView.overlays.add(Marker(mapView).apply {
                    position = GeoPoint(wp.latitude, wp.longitude); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createPinDrawable(context, pinColor, wp.name, wp.icon); id = wp.id; setInfoWindow(null)
                    setOnMarkerClickListener { m, _ -> vibrate(context, light = true); selectedWaypoint = waypoints.find { it.id == m.id }; refreshOverlays(mapView); true }
                })
            } else {
                mapView.overlays.add(Marker(mapView).apply {
                    position = GeoPoint(cluster.centerLat, cluster.centerLon); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createClusterDrawable(context, cluster.items.size); setInfoWindow(null)
                    setOnMarkerClickListener { _, mv -> mv.controller.animateTo(position, mv.zoomLevelDouble + 2.0, 400); true }
                })
            }
        }

        // User dot
        userLocation?.let { loc ->
            mapView.overlays.add(Marker(mapView).apply {
                position = loc; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createUserDotDrawable(context); setInfoWindow(null); setOnMarkerClickListener { _, _ -> true }
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
                minZoomLevel = 3.0; maxZoomLevel = 20.0; controller.setZoom(15.0); controller.setCenter(GeoPoint(51.5074, -0.1278))
                overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        if (measureMode) return false; selectedWaypoint = null; refreshOverlays(this@apply); return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean {
                        vibrate(ctx); val wp = Waypoint(name = "Waypoint ${waypoints.size + 1}", latitude = p.latitude, longitude = p.longitude)
                        waypoints = waypoints + wp; store.save(waypoints); refreshOverlays(this@apply); return true
                    }
                }))
                overlays.add(RotationGestureOverlay(this))
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?) = false.also {
                        mapRotation = mapOrientation
                        projection?.let { p -> val px = p.metersToPixels(1000f); if (px > 0) metersPerPx = 1000f / px }
                    }
                    override fun onZoom(event: ZoomEvent?) = false.also {
                        refreshOverlays(this@apply)
                        mapRotation = mapOrientation
                        projection?.let { p -> val px = p.metersToPixels(1000f); if (px > 0) metersPerPx = 1000f / px }
                    }
                })
                mapViewRef.value = this
            }
        })

        // ── Top-left: compass + coordinate header ─────────────────
        Row(
            modifier = Modifier.statusBarsPadding().padding(start = 12.dp, top = 12.dp).align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComposeCompass(rotation = mapRotation, onClick = {
                vibrate(context, light = true)
                mapViewRef.value?.apply { mapOrientation = 0f; invalidate() }
                mapRotation = 0f
            })
            CoordinateHeader(
                userLocation = userLocation, locationEnabled = locationEnabled, coordFormat = coordFormat,
                onToggleFormat = { coordFormat = CoordFormat.entries[(coordFormat.ordinal + 1) % CoordFormat.entries.size]; store.saveCoordFormat(coordFormat.name) },
            )
        }

        // ── Top-right: 3 buttons ────────────────────────────────
        Column(
            modifier = Modifier.statusBarsPadding().padding(end = 16.dp, top = 12.dp).align(Alignment.TopEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                MapButton(Icons.Filled.Layers, "Map style") { showStyleMenu = true }
                DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    MapStyle.entries.forEach { style ->
                        DropdownMenuItem(text = { Text(style.label, fontSize = 15.sp) }, onClick = {
                            mapStyle = style; store.saveMapStyle(style.name); mapViewRef.value?.let { it.setTileSource(style.tileSource()); it.invalidate() }; showStyleMenu = false
                        })
                    }
                }
            }
            MapButton(Icons.AutoMirrored.Filled.List, "Waypoints") { showWaypointList = true }
            Box {
                MapButton(Icons.Filled.MoreVert, "More") { showOverflowMenu = true }
                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    DropdownMenuItem(text = { Text("Export GPX", fontSize = 15.sp) }, onClick = { showOverflowMenu = false; exportGpxLauncher.launch("waypoints.gpx") })
                    DropdownMenuItem(text = { Text("Import GPX", fontSize = 15.sp) }, onClick = { showOverflowMenu = false; importGpxLauncher.launch(arrayOf("*/*")) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 12.dp))
                    DropdownMenuItem(text = { Text("Backup all data", fontSize = 15.sp) }, onClick = { showOverflowMenu = false; backupExportLauncher.launch("waypoint-backup.zip") })
                    DropdownMenuItem(text = { Text("Restore backup", fontSize = 15.sp) }, onClick = { showOverflowMenu = false; backupImportLauncher.launch(arrayOf("*/*")) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 12.dp))
                    DropdownMenuItem(text = { Text(if (measureMode) "Stop measuring" else "Measure distance", fontSize = 15.sp) }, onClick = {
                        showOverflowMenu = false; measureMode = !measureMode
                        if (measureMode) { mapViewRef.value?.let { mv -> val ov = MeasureOverlay { measureResult = it }; ov.activate(); measureOverlayRef.value = ov; mv.overlays.add(ov); mv.invalidate() } }
                        else { measureOverlayRef.value?.let { mapViewRef.value?.overlays?.remove(it); mapViewRef.value?.invalidate() }; measureOverlayRef.value = null; measureResult = null }
                    })
                    DropdownMenuItem(text = { Text("Download area", fontSize = 15.sp) }, onClick = {
                        showOverflowMenu = false; val map = mapViewRef.value ?: return@DropdownMenuItem
                        val ts = map.tileProvider.tileSource; if (ts !is OnlineTileSourceBase) return@DropdownMenuItem
                        val bb = map.boundingBox; val zoom = map.zoomLevelDouble.toInt(); val maxZ = minOf(zoom + 2, 19)
                        downloadProgress = 0.01f; val handler = Handler(Looper.getMainLooper())
                        Thread { try { val cm = CacheManager(map); val tiles = CacheManager.getTilesCoverage(bb, zoom, maxZ); val total = tiles.size.coerceAtLeast(1); var done = 0
                            for (tile in tiles) { cm.loadTile(ts, tile); done++; if (done % 5 == 0 || done == total) handler.post { downloadProgress = done.toFloat() / total } }
                        } catch (_: Exception) { }; handler.post { downloadProgress = null } }.start()
                    })
                }
            }
        }

        // ── Measure result banner ───────────────────────────────
        measureResult?.let { result ->
            Surface(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp),
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp
            ) { Text(result, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onPrimary) }
        }

        // ── Bottom-left: scale bar (hidden when card open) ──
        if (selectedWaypoint == null) {
            ComposeScaleBar(
                metersPerPx = metersPerPx,
                modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 16.dp, bottom = 24.dp)
            )
        }

        // ── Download progress ───────────────────────────────────
        downloadProgress?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).align(Alignment.Center))
        }

        // ── Hint capsule (centered at bottom) ───────────────────
        if (waypoints.isEmpty() && selectedWaypoint == null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp)) {
                HintCapsule()
            }
        }

        // ── Bottom row ──────────────────────────────────────────
        Row(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.Bottom) {
            // Card
            Box(modifier = Modifier.weight(1f)) {
                val sel = selectedWaypoint
                if (sel != null) {
                    WaypointCard(waypoint = sel,
                        distance = userLocation?.let { formatDistance(distanceMeters(it, GeoPoint(sel.latitude, sel.longitude))) },
                        onEdit = { showEditSheet = true }, onDelete = { pendingDelete = sel }, onClose = { selectedWaypoint = null })
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Right column: controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Record (with optional pause)
                if (trackRecorder.isRecording) {
                    MapButton(if (trackRecorder.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        if (trackRecorder.isPaused) "Resume" else "Pause") { vibrate(context, light = true); if (trackRecorder.isPaused) trackRecorder.resume() else trackRecorder.pause() }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                RecordButton(isRecording = trackRecorder.isRecording, isPaused = trackRecorder.isPaused) {
                    vibrate(context)
                    if (trackRecorder.isRecording) { val t = trackRecorder.stop(); if (t.points.size >= 2) { tracks = tracks + t; store.saveTracks(tracks) } }
                    else trackRecorder.start()
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Zoom pill (Apple Maps style)
                ZoomPill(
                    onZoomIn = { vibrate(context, light = true); mapViewRef.value?.controller?.zoomIn() },
                    onZoomOut = { vibrate(context, light = true); mapViewRef.value?.controller?.zoomOut() }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Recenter
                RecenterButton(enabled = locationEnabled) {
                    vibrate(context, light = true); userLocation?.let { mapViewRef.value?.controller?.animateTo(it, 18.5, 600) }
                }
            }
        }

        // Undo toast
        androidx.compose.animation.AnimatedVisibility(
            visible = showUndoToast,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp)
        ) {
            Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), shadowElevation = 6.dp) {
                Row(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Waypoint deleted", color = MaterialTheme.colorScheme.surface, fontSize = 14.sp, fontWeight = FontWeight.W500)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Undo", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.W600,
                        modifier = Modifier.iosClickable {
                            lastDeleted?.let { waypoints = waypoints + it; store.save(waypoints); lastDeleted = null }
                            showUndoToast = false
                        }.padding(horizontal = 10.dp, vertical = 8.dp))
                }
            }
        }
    }

    // ── Sheets & dialogs ────────────────────────────────────────
    pendingDelete?.let { wp ->
        DeleteConfirmSheet(waypointName = wp.name, onConfirm = {
            wp.photoPath?.let { File(it).delete() }
            waypoints = waypoints.filter { it.id != wp.id }; store.save(waypoints); selectedWaypoint = null; lastDeleted = wp; pendingDelete = null
            showUndoToast = true
            scope.launch { kotlinx.coroutines.delay(4000); showUndoToast = false }
        }, onDismiss = { pendingDelete = null })
    }

    if (showEditSheet) {
        selectedWaypoint?.let { wp ->
            EditWaypointSheet(waypoint = wp, onDismiss = { showEditSheet = false },
                onSave = { name, notes, color, photoPath, icon ->
                    val s = name.trim().take(64).ifBlank { "Unnamed Waypoint" }
                    waypoints = waypoints.map { if (it.id == wp.id) it.copy(name = s, notes = notes.trim(), color = color, photoPath = photoPath, icon = icon) else it }
                    store.save(waypoints); selectedWaypoint = waypoints.find { it.id == wp.id }; showEditSheet = false; vibrate(context, light = true)
                })
        }
    }

    if (showWaypointList) {
        WaypointListSheet(waypoints = waypoints, tracks = tracks, userLocation = userLocation,
            onSelectWaypoint = { wp -> selectedWaypoint = wp; mapViewRef.value?.controller?.animateTo(GeoPoint(wp.latitude, wp.longitude), 18.0, 800); showWaypointList = false },
            onSelectTrack = { track -> selectedTrack = track; if (track.points.isNotEmpty()) { val c = GeoPoint(track.points.sumOf { it.latitude } / track.points.size, track.points.sumOf { it.longitude } / track.points.size); mapViewRef.value?.controller?.animateTo(c, 15.0, 800) }; showWaypointList = false },
            onDeleteTrack = { track -> tracks = tracks.filter { it.id != track.id }; store.saveTracks(tracks) },
            onReorderWaypoints = { r -> waypoints = r; store.save(r); store.saveWaypointOrder(r.map { it.id }) },
            onDismiss = { showWaypointList = false })
    }

    selectedTrack?.let { track ->
        TrackDetailSheet(track = track, onDelete = { tracks = tracks.filter { it.id != track.id }; store.saveTracks(tracks); selectedTrack = null }, onDismiss = { selectedTrack = null })
    }
}

// ── iOS-style map button ────────────────────────────────────────
@Composable
private fun MapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), shadowElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().iosClickable(onClick), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Zoom pill (Apple Maps style) ────────────────────────────────
@Composable
private fun ZoomPill(onZoomIn: () -> Unit, onZoomOut: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), shadowElevation = 4.dp) {
        Column(modifier = Modifier.width(44.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(44.dp).iosClickable(onZoomIn), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, "Zoom in", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(44.dp).iosClickable(onZoomOut), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Remove, "Zoom out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RecenterButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), shadowElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().iosClickable { if (enabled) onClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MyLocation, "Center", tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, isPaused: Boolean = false, onClick: () -> Unit) {
    val bg = when { isRecording && isPaused -> Color(0xFFFF2D55).copy(alpha = 0.45f); isRecording -> Color(0xFFFF2D55); else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.88f) }
    Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = bg, shadowElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().iosClickable(onClick), contentAlignment = Alignment.Center) {
            if (isRecording) Icon(Icons.Filled.Stop, "Stop", tint = Color.White, modifier = Modifier.size(22.dp))
            else Icon(Icons.Filled.FiberManualRecord, "Record", tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Compose compass (Apple Maps style) ──────────────────────────
@Composable
private fun ComposeCompass(rotation: Float, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().iosClickable(onClick), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(20.dp).rotate(-rotation)) {
                val cx = size.width / 2; val cy = size.height / 2
                // Red north half
                val northPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, 0f); lineTo(cx + 5.dp.toPx(), cy); lineTo(cx - 5.dp.toPx(), cy); close()
                }
                drawPath(northPath, Color(0xFFFF3B30))
                // White south half
                val southPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, size.height); lineTo(cx + 5.dp.toPx(), cy); lineTo(cx - 5.dp.toPx(), cy); close()
                }
                drawPath(southPath, Color(0xFFE5E5EA))
            }
        }
    }
}

// ── Compose scale bar (minimal iOS style) ───────────────────────
@Composable
private fun ComposeScaleBar(metersPerPx: Float, modifier: Modifier = Modifier) {
    val targetPx = 80f
    val rawMeters = metersPerPx * targetPx
    val niceMeters = niceRound(rawMeters)
    val barWidth = (niceMeters / metersPerPx).coerceIn(30f, 150f)
    val label = if (niceMeters >= 1000) "${"%.0f".format(niceMeters / 1000)} km" else "${"%.0f".format(niceMeters)} m"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(modifier = Modifier.width(barWidth.dp).height(6.dp)) {
                val h = size.height; val w = size.width; val stroke = 1.5.dp.toPx()
                drawLine(Color.White, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = stroke)
                drawLine(Color.White, Offset(0f, 0f), Offset(0f, h), strokeWidth = stroke)
                drawLine(Color.White, Offset(w, 0f), Offset(w, h), strokeWidth = stroke)
            }
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.W600, color = Color.White)
        }
    }
}

private fun niceRound(meters: Float): Float = when {
    meters >= 5000 -> (meters / 1000).toInt().toFloat() * 1000
    meters >= 1000 -> ((meters / 500).toInt() * 500).toFloat()
    meters >= 200 -> ((meters / 100).toInt() * 100).toFloat()
    meters >= 50 -> ((meters / 50).toInt() * 50).toFloat()
    meters >= 10 -> ((meters / 10).toInt() * 10).toFloat()
    else -> ((meters / 5).toInt() * 5).toFloat().coerceAtLeast(5f)
}
