package com.florian.piste

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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


/**
 * The app's main — and only — screen.
 *
 * Hosts the osmdroid map via [AndroidView] with Compose overlays on top:
 * compass, coordinate header, weather pill, home arrow pill, top-right button
 * column (map style, waypoint list, daily stats, overflow menu), bottom-right
 * controls (record, pause, zoom pill, recenter), the waypoint card, scale bar,
 * achievement banner, undo toast, and the onboarding overlay on first launch.
 *
 * Persistence is delegated to [PisteRepository] via [PisteViewModel].
 * Waypoints and tracks are observed as [StateFlow]s from the repository.
 * UI-only state (selected waypoint, sheet visibility, etc.) lives in local
 * [remember] blocks.
 *
 * @param viewModel The [PisteViewModel] providing repository access and
 *   location / weather state.
 * @param onToggleGlare Called when the user toggles glare mode in settings; the
 *   caller should update the theme wrapping this composable.
 */
@Composable
fun MapScreen(viewModel: PisteViewModel, onToggleGlare: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glareMode = LocalGlareMode.current
    val repo = viewModel.repository

    // ── Core state ──────────────────────────────────────────────
    val waypoints by repo.waypoints.collectAsStateWithLifecycle()
    val tracks by repo.tracks.collectAsStateWithLifecycle()
    var selectedWaypoint by remember { mutableStateOf<Waypoint?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationEnabled by remember { mutableStateOf(false) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // ── UI state ────────────────────────────────────────────────
    var showEditSheet by remember { mutableStateOf(false) }
    var showWaypointList by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDailyStats by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var mapStyle by remember { mutableStateOf(runCatching { MapStyle.valueOf(repo.loadMapStyle()) }.getOrDefault(MapStyle.STANDARD)) }
    var userManuallyChangedStyle by remember { mutableStateOf(false) }

    // Weather
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var lastWeatherFetch by remember { mutableStateOf(0L) }
    var lastForecastFetch by remember { mutableStateOf(0L) }

    // Daylight remaining — recomputed every minute while location is known
    var minutesToSunset by remember { mutableStateOf<Int?>(null) }
    var showNightSkiingPill by remember { mutableStateOf(false) }

    // Avalanche awareness — computed from the hourly forecast
    var awareness by remember { mutableStateOf<AwarenessAssessment?>(null) }
    var showAwarenessSheet by remember { mutableStateOf(false) }

    // Delete flow
    var pendingDelete by remember { mutableStateOf<Waypoint?>(null) }
    var lastDeleted by remember { mutableStateOf<Waypoint?>(null) }
    var showUndoToast by remember { mutableStateOf(false) }

    // Track detail
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    // Settings (re-read from prefs each recomposition so changes take effect immediately)
    var showSettings by remember { mutableStateOf(false) }
    var settingsVersion by remember { mutableIntStateOf(0) } // bump to force re-read
    val useImperial = remember(settingsVersion) { repo.loadSetting("distance_unit", "METRIC") == "IMPERIAL" }
    var lastProximityAlert by remember { mutableStateOf(0L) }
    var lastSpeedAlert by remember { mutableStateOf(0L) }
    var lastLocationForBearing by remember { mutableStateOf<GeoPoint?>(null) }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var mapRotation by remember { mutableFloatStateOf(0f) }
    var metersPerPx by remember { mutableFloatStateOf(1f) }
    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var currentAltitude by remember { mutableStateOf<Double?>(null) }
    var autoFollow by remember { mutableStateOf(false) }
    var showSpeedometer by remember { mutableStateOf(false) }
    var achievementBanner by remember { mutableStateOf<String?>(null) }
    var placeName by remember { mutableStateOf<String?>(null) }
    var lastPlaceFetchLoc by remember { mutableStateOf<GeoPoint?>(null) }
    var showCompare by remember { mutableStateOf(false) }
    var recapTrack by remember { mutableStateOf<Track?>(null) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    var forecast by remember { mutableStateOf<Forecast?>(null) }
    var showOnboarding by remember { mutableStateOf(repo.loadSetting("has_onboarded", "false") != "true") }
    var showNotificationRationale by remember { mutableStateOf(false) }
    var showBatteryPrompt by remember { mutableStateOf(false) }
    var showRecordingInterrupted by remember { mutableStateOf(false) }
    var homeId by remember { mutableStateOf(repo.loadHomeId()) }
    val trackRecorder = remember { TrackRecorder() }
    val airDetector = remember { AirTimeDetector(context) { durationMs -> trackRecorder.addAirTime(durationMs) } }

    // ── Location ────────────────────────────────────────────────
    val gmsAvailable = remember {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    val locationClient = remember {
        if (gmsAvailable) LocationServices.getFusedLocationProviderClient(context) else null
    }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val gp = GeoPoint(loc.latitude, loc.longitude)
                userLocation = gp
                locationEnabled = true
                currentSpeed = if (loc.hasSpeed()) loc.speed else 0f
                if (loc.hasAltitude() && loc.altitude != 0.0) currentAltitude = loc.altitude
                if (trackRecorder.isRecording) trackRecorder.addPoint(loc.latitude, loc.longitude, loc.altitude)

                // Auto-follow: keep map centered while recording
                if (autoFollow && trackRecorder.isRecording) {
                    mapViewRef.value?.controller?.animateTo(gp)
                }

                // Compute user bearing from consecutive location updates
                val userBearing = lastLocationForBearing?.let { prev -> bearingDegrees(prev, gp) }
                lastLocationForBearing = gp

                // Proximity alerts — vibrate when within the configured radius of a waypoint
                // Only alerts for waypoints roughly ahead (within 90 degrees of travel direction)
                val radius = repo.loadSetting("proximity_radius", "0").toIntOrNull() ?: 0
                if (radius > 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastProximityAlert > 15000) { // max once per 15s
                        for (wp in waypoints) {
                            val dist = distanceMeters(gp, GeoPoint(wp.latitude, wp.longitude))
                            if (dist < radius) {
                                // Only alert if waypoint is roughly ahead (within 90 degrees of travel direction)
                                if (userBearing != null) {
                                    val wpBearing = bearingDegrees(gp, GeoPoint(wp.latitude, wp.longitude))
                                    val angleDiff = ((wpBearing - userBearing + 360) % 360).let { if (it > 180) 360 - it else it }
                                    if (angleDiff > 90) continue // behind us, skip
                                }
                                Haptics.proximity(context); lastProximityAlert = now; break
                            }
                        }
                    }
                }

                // Speed alerts — vibrate when exceeding the configured speed threshold
                val speedAlertKmh = repo.loadSetting("speed_alert_kmh", "0").toIntOrNull() ?: 0
                if (speedAlertKmh > 0 && loc.hasSpeed()) {
                    val currentKmh = loc.speed * 3.6f
                    val now = System.currentTimeMillis()
                    if (currentKmh > speedAlertKmh && now - lastSpeedAlert > 30_000) {
                        Haptics.warning(context)
                        lastSpeedAlert = now
                    }
                }
            }
        }
    }
    /** Build a location request with the given [intervalMs]. */
    fun buildLocationRequest(intervalMs: Long) = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, intervalMs
    ).setMinUpdateDistanceMeters(2f).build()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it } && locationClient != null) {
            try {
                locationClient.requestLocationUpdates(buildLocationRequest(3000), locationCallback, Looper.getMainLooper())
            } catch (_: SecurityException) { }
        }
    }

    // Bug 6: Reduce location frequency when paused to save battery
    LaunchedEffect(trackRecorder.isPaused) {
        if (locationClient == null) return@LaunchedEffect
        try {
            val interval = if (trackRecorder.isPaused) 10_000L else 3_000L
            locationClient.removeLocationUpdates(locationCallback)
            locationClient.requestLocationUpdates(buildLocationRequest(interval), locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) { }
    }

    // ── SAF launchers ───────────────────────────────────────────
    val exportGpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(exportGpx(waypoints, tracks).toByteArray()) } }
    }
    val importGpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val xml = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@let
            val data = importGpx(xml)
            if (data.waypoints.isNotEmpty()) { repo.importWaypoints(data.waypoints) }
            if (data.tracks.isNotEmpty()) { repo.importTracks(data.tracks) }
        }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> createBackupZip(context, os) } }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { ins ->
                if (restoreBackupZip(context, ins)) {
                    repo.reload()
                    mapStyle = runCatching { MapStyle.valueOf(repo.loadMapStyle()) }.getOrDefault(MapStyle.STANDARD)
                }
            }
        }
    }

    // Separate launcher for notification permission (used after rationale dialog)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    // ── Init ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply { userAgentValue = "com.florian.piste"; tileFileSystemCacheMaxBytes = 100L * 1024 * 1024; tileFileSystemCacheTrimBytes = 80L * 1024 * 1024 }
        // Request location permissions immediately
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionLauncher.launch(perms.toTypedArray())
        // On Android 13+, show rationale dialog before requesting notification permission
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                showNotificationRationale = true
            }
        }
    }
    // Check if a previous recording was interrupted (e.g. force-kill)
    LaunchedEffect(Unit) {
        if (TrackRecorder.wasRecordingInterrupted(context)) {
            showRecordingInterrupted = true
            TrackRecorder.markRecordingInactive(context)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            locationClient?.removeLocationUpdates(locationCallback)
            airDetector.stop()
        }
    }

    // Battery optimization — one-time prompt after location is set up
    LaunchedEffect(locationEnabled) {
        if (!locationEnabled) return@LaunchedEffect
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName) && repo.loadSetting("battery_prompt_shown", "false") != "true") {
            showBatteryPrompt = true
        }
    }

    // Keep screen on while recording (if enabled in settings)
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(trackRecorder.isRecording, settingsVersion) {
        val keepOn = repo.loadSetting("keep_screen_on", "true") == "true"
        view.keepScreenOn = trackRecorder.isRecording && keepOn
        onDispose { view.keepScreenOn = false }
    }

    // Resort / place detection via Nominatim, refetch when moved > 5km
    LaunchedEffect(userLocation) {
        val loc = userLocation ?: return@LaunchedEffect
        val last = lastPlaceFetchLoc
        if (last == null || distanceMeters(last, loc) > 5000) {
            lastPlaceFetchLoc = loc
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val name = fetchPlaceName(loc.latitude, loc.longitude)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { placeName = name }
            }
        }
    }

    // ── Refresh overlays ────────────────────────────────────────
    fun refreshOverlays(mapView: MapView) {
        mapView.overlays.removeAll { it is Marker || it is Polyline }
        val density = context.resources.displayMetrics.density

        // Tracks (speed heatmap for the selected one, solid color for others)
        for (track in tracks) {
            if (track.id == selectedTrack?.id) {
                addSpeedHeatmap(mapView, track.points, density)
            } else {
                mapView.overlays.add(Polyline(mapView).apply {
                    setPoints(track.points.map { GeoPoint(it.latitude, it.longitude) })
                    outlinePaint.color = safeParseColor(track.color)
                    outlinePaint.strokeWidth = 3f * density; outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND; outlinePaint.isAntiAlias = true
                })
            }
        }
        val recordingPoints = trackRecorder.currentPoints.toList()
        if (recordingPoints.isNotEmpty()) {
            mapView.overlays.add(Polyline(mapView).apply {
                setPoints(recordingPoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = RecordingTrackColor.toArgb(); outlinePaint.strokeWidth = 3.5f * density
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND; outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND; outlinePaint.isAntiAlias = true
            })
        }

        // Clustered waypoint markers
        val clusters = clusterWaypoints(waypoints, mapView)
        for (cluster in clusters) {
            if (cluster.items.size == 1) {
                val wp = cluster.items.first(); val isSelected = wp.id == selectedWaypoint?.id
                val pinColor = if (isSelected) SelectedPinColor else Color(safeParseColor(wp.color))
                mapView.overlays.add(Marker(mapView).apply {
                    position = GeoPoint(wp.latitude, wp.longitude); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createPinDrawable(context, pinColor, wp.name, wp.icon); id = wp.id; setInfoWindow(null)
                    setOnMarkerClickListener { m, _ -> Haptics.tap(context); selectedWaypoint = waypoints.find { it.id == m.id }; refreshOverlays(mapView); true }
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
        if (!hasCenteredOnUser) {
            val defZoom = repo.loadSetting("default_zoom", "15").toDoubleOrNull() ?: 15.0
            map.controller.animateTo(loc, defZoom, 600); hasCenteredOnUser = true
        }
    }
    LaunchedEffect(waypoints, selectedWaypoint, tracks) { mapViewRef.value?.let { refreshOverlays(it) } }
    LaunchedEffect(mapStyle) { mapViewRef.value?.let { it.setTileSource(mapStyle.tileSource()); it.invalidate() } }

    // Auto dark map — switch map tiles when system dark mode changes
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    LaunchedEffect(isSystemDark, settingsVersion) {
        val autoDark = repo.loadSetting("auto_dark_map", "true") == "true"
        if (!autoDark || userManuallyChangedStyle) return@LaunchedEffect
        if (isSystemDark && mapStyle == MapStyle.STANDARD) {
            mapStyle = MapStyle.DARK; repo.saveMapStyle(MapStyle.DARK.name)
        } else if (!isSystemDark && mapStyle == MapStyle.DARK) {
            mapStyle = MapStyle.STANDARD; repo.saveMapStyle(MapStyle.STANDARD.name)
        }
    }

    // Night skiing mode — after sunset, suggest dark map or auto-switch
    LaunchedEffect(minutesToSunset) {
        val mins = minutesToSunset ?: return@LaunchedEffect
        if (mins < 0 && mapStyle != MapStyle.DARK) {
            // Auto-switch to dark if auto_dark_map is enabled and user hasn't manually changed style
            val autoDark = repo.loadSetting("auto_dark_map", "true") == "true"
            if (autoDark && !userManuallyChangedStyle) {
                mapStyle = MapStyle.DARK; repo.saveMapStyle(MapStyle.DARK.name)
            } else {
                // Show a pill suggesting dark mode
                showNightSkiingPill = true
            }
        } else if (mins >= 0) {
            showNightSkiingPill = false
        }
    }

    // Weather — fetch when location is known, refresh every 15 minutes
    LaunchedEffect(userLocation) {
        val loc = userLocation ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastWeatherFetch > 15 * 60_000) {
            lastWeatherFetch = now
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val w = fetchWeather(loc.latitude, loc.longitude)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { weather = w }
            }
        }
    }

    // Daylight remaining — refresh every 60s from pure local math
    LaunchedEffect(userLocation) {
        while (true) {
            val loc = userLocation
            minutesToSunset = loc?.let { minutesUntilSunset(it.latitude, it.longitude) }
            kotlinx.coroutines.delay(60_000)
        }
    }

    // Forecast for avalanche awareness — fetched silently every 30 minutes
    LaunchedEffect(userLocation) {
        val loc = userLocation ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastForecastFetch > 30 * 60_000) {
            lastForecastFetch = now
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val f = fetchForecast(loc.latitude, loc.longitude)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    forecast = f
                    awareness = assessAvalancheAwareness(f)
                }
            }
        }
    }

    // ── UI ──────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(mapStyle.tileSource()); setMultiTouchControls(true)
                @Suppress("DEPRECATION") setBuiltInZoomControls(false)
                val defZoom = repo.loadSetting("default_zoom", "15").toDoubleOrNull() ?: 15.0
                minZoomLevel = 3.0; maxZoomLevel = 20.0; controller.setZoom(defZoom); controller.setCenter(GeoPoint(51.5074, -0.1278))
                overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        selectedWaypoint = null; refreshOverlays(this@apply); return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean {
                        Haptics.mediumTap(ctx); val wp = Waypoint(name = "Waypoint ${waypoints.size + 1}".take(64), latitude = p.latitude, longitude = p.longitude)
                        repo.addWaypoint(wp); refreshOverlays(this@apply); return true
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
                // User-touch listener — any manual interaction turns off auto-follow
                setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        autoFollow = false
                    }
                    false
                }
                mapViewRef.value = this
            }
        })

        // ── Top-left: compass + coordinate header + weather ─────
        Column(
            modifier = Modifier.statusBarsPadding().padding(start = 12.dp, top = 12.dp, end = 72.dp).align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ComposeCompass(rotation = mapRotation, onClick = {
                    Haptics.tap(context)
                    mapViewRef.value?.apply { mapOrientation = 0f; invalidate() }
                    mapRotation = 0f
                })
                LocationHeader(
                    placeName = placeName,
                    locationEnabled = locationEnabled,
                    altitudeText = currentAltitude?.let { formatVertical(it, useImperial) },
                    speedText = formatSpeed(currentSpeed, useImperial),
                    onLongPress = { showSpeedometer = true },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            // Daylight pill — only shown when sunset is within 90 minutes
            minutesToSunset?.let { mins ->
                if (mins in 0..90) {
                    DaylightPill(minutesLeft = mins)
                }
            }

            // Night skiing pill — shown after sunset (when recording, or as a suggestion to switch to dark map)
            if (showNightSkiingPill || (trackRecorder.isRecording && minutesToSunset != null && minutesToSunset!! < 0)) {
                NightSkiingPill(onClick = {
                    mapStyle = MapStyle.DARK; repo.saveMapStyle(MapStyle.DARK.name)
                    showNightSkiingPill = false
                })
            }

            // Avalanche awareness pill — only shown when conditions are elevated
            awareness?.takeIf { it.elevated }?.let {
                AwarenessPill(onClick = { showAwarenessSheet = true })
            }

            weather?.let { w ->
                val wind = formatWind(w.windSpeedKmh, useImperial)
                val tempStr = formatTemperature(w.temperatureC, useImperial)
                val warn = w.windSpeedKmh >= WindClosureThresholdKmh
                WeatherPill(
                    text = "${weatherEmoji(w.weatherCode)} $tempStr \u00B7 $wind",
                    warning = warn,
                    onClick = {
                        showWeatherSheet = true
                        // Fetch forecast on demand
                        userLocation?.let { loc ->
                            scope.launch {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val f = fetchForecast(loc.latitude, loc.longitude)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { forecast = f }
                                }
                            }
                        }
                    }
                )
            }

            // Home arrow pill — points back to the home waypoint
            val home = homeId?.let { id -> waypoints.find { it.id == id } }
            val loc = userLocation
            if (home != null && loc != null) {
                val homePoint = GeoPoint(home.latitude, home.longitude)
                val dist = distanceMeters(loc, homePoint)
                val bearing = bearingDegrees(loc, homePoint) - mapRotation
                HomeArrowPill(
                    distanceText = formatDistance(dist, useImperial).replace(" away", ""),
                    bearing = bearing,
                    onClick = {
                        mapViewRef.value?.controller?.animateTo(homePoint, 16.0, 800)
                    }
                )
            }
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
                            mapStyle = style; repo.saveMapStyle(style.name); userManuallyChangedStyle = true; mapViewRef.value?.let { it.setTileSource(style.tileSource()); it.invalidate() }; showStyleMenu = false
                        })
                    }
                }
            }
            MapButton(Icons.AutoMirrored.Filled.List, "Waypoints") { showWaypointList = true }
            MapButton(Icons.Filled.Terrain, "Today's Stats") { showDailyStats = true }
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
                    DropdownMenuItem(text = { Text("Download area", fontSize = 15.sp) }, onClick = {
                        showOverflowMenu = false; val map = mapViewRef.value ?: return@DropdownMenuItem
                        val ts = map.tileProvider.tileSource; if (ts !is OnlineTileSourceBase) return@DropdownMenuItem
                        val bb = map.boundingBox; val zoom = map.zoomLevelDouble.toInt(); val maxZ = minOf(zoom + 2, 19)
                        downloadProgress = 0.01f; val handler = Handler(Looper.getMainLooper())
                        val startTime = System.currentTimeMillis()
                        Thread {
                            try {
                                val cm = CacheManager(map); val tiles = CacheManager.getTilesCoverage(bb, zoom, maxZ)
                                val total = tiles.size.coerceAtLeast(1); var done = 0
                                for (tile in tiles) {
                                    cm.loadTile(ts, tile); done++
                                    if (done % 3 == 0 || done == total) handler.post { downloadProgress = done.toFloat() / total }
                                }
                            } catch (_: Exception) { }
                            // Keep visible for at least 1.5s so user sees it
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 1500) Thread.sleep(1500 - elapsed)
                            handler.post { downloadProgress = null }
                        }.start()
                    })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 12.dp))
                    DropdownMenuItem(text = { Text("Settings", fontSize = 15.sp) }, onClick = { showOverflowMenu = false; showSettings = true })
                }
            }
        }

        // ── Measure result banner ───────────────────────────────
        // ── Bottom-left: scale bar (hidden when card open) ──
        if (selectedWaypoint == null) {
            ComposeScaleBar(
                metersPerPx = metersPerPx,
                imperial = useImperial,
                modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 16.dp, bottom = 24.dp)
            )
        }

        // ── Download progress pill ──────────────────────────────
        downloadProgress?.let { progress ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Downloading tiles\u2026", fontSize = 14.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.width(180.dp).height(4.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
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
                        distance = userLocation?.let { formatDistance(distanceMeters(it, GeoPoint(sel.latitude, sel.longitude)), useImperial) },
                        isHome = homeId == sel.id,
                        onToggleHome = {
                            homeId = if (homeId == sel.id) null else sel.id
                            repo.saveHomeId(homeId)
                        },
                        onEdit = { showEditSheet = true }, onDelete = { pendingDelete = sel }, onClose = { selectedWaypoint = null })
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Right column: controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Record (with optional pause)
                if (trackRecorder.isRecording) {
                    MapButton(if (trackRecorder.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        if (trackRecorder.isPaused) "Resume" else "Pause") {
                        Haptics.tap(context)
                        if (trackRecorder.isPaused) trackRecorder.resume() else trackRecorder.pause()
                        TrackingService.refresh(context) // update notification state
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val stopRecording: () -> Unit = {
                    airDetector.stop()
                    TrackRecorder.markRecordingInactive(context)
                    val raw = trackRecorder.stop()
                    // Auto-name with resort if available
                    val dateFmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                    val niceName = if (placeName != null) "${placeName} \u2014 ${dateFmt.format(java.util.Date(raw.startTime))}"
                    else raw.name
                    val t = raw.copy(name = niceName)
                    if (t.points.size >= 2) {
                        repo.addTrack(t)
                        // Check achievements against today's totals
                        val dayStart = startOfDay()
                        val todayTracks = repo.tracks.value.filter { it.startTime >= dayStart }
                        var dayVert = 0.0; var dayRuns = 0; var dayDist = 0.0; var dayMax = 0.0; var dayDur = 0L
                        for (tt in todayTracks) {
                            val s = computeTrackStatsCached(tt)
                            dayVert += s.verticalDescended ?: 0.0
                            dayRuns += s.runCount
                            dayDist += s.distanceMeters
                            if (s.maxSpeedKmh > dayMax) dayMax = s.maxSpeedKmh
                            dayDur += s.durationMs
                        }
                        val achievements = repo.checkAchievements(dayMax, dayVert, dayRuns, dayDist)
                        if (achievements.isNotEmpty()) {
                            achievementBanner = achievements.first()
                            Haptics.personalBest(context)
                            scope.launch { kotlinx.coroutines.delay(4000); achievementBanner = null }
                        }
                        // Refresh home screen widget
                        StatsWidget.refreshAll(context)
                        // Ring-closed celebration — fire haptic + sound when any goal is met
                        val vertGoal = repo.loadSetting("goal_vertical_m", "3000").toDoubleOrNull() ?: 3000.0
                        val runsGoal = repo.loadSetting("goal_runs", "10").toIntOrNull() ?: 10
                        val timeGoalMs = (repo.loadSetting("goal_time_min", "240").toLongOrNull() ?: 240L) * 60_000L
                        if (dayVert >= vertGoal || dayRuns >= runsGoal || dayDur >= timeGoalMs) {
                            Haptics.ringClosed(context)
                        }
                        // Auto-show recap for sessions of 45+ minutes
                        if ((t.endTime - t.startTime) >= 45 * 60_000L) {
                            recapTrack = t
                        }
                    }
                    autoFollow = false
                    RecordingBridge.recorder = null
                    RecordingBridge.onStop = null
                    try { context.stopService(android.content.Intent(context, TrackingService::class.java)) } catch (_: Exception) { }
                }

                // Register the stop callback so the notification "Stop" button can invoke it
                DisposableEffect(trackRecorder.isRecording) {
                    if (trackRecorder.isRecording) {
                        RecordingBridge.recorder = trackRecorder
                        RecordingBridge.onStop = stopRecording
                    }
                    onDispose {
                        RecordingBridge.recorder = null
                        RecordingBridge.onStop = null
                    }
                }

                RecordButton(isRecording = trackRecorder.isRecording, isPaused = trackRecorder.isPaused) {
                    if (trackRecorder.isRecording) {
                        Haptics.recordStop(context)
                        stopRecording()
                    } else {
                        Haptics.recordStart(context)
                        trackRecorder.start()
                        TrackRecorder.markRecordingActive(context)
                        if (repo.loadSetting("air_time_enabled", "true") == "true") {
                            airDetector.start()
                        }
                        autoFollow = true
                        RecordingBridge.recorder = trackRecorder
                        RecordingBridge.onStop = stopRecording
                        try { context.startForegroundService(android.content.Intent(context, TrackingService::class.java)) } catch (_: Exception) { }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Zoom pill (Apple Maps style)
                ZoomPill(
                    onZoomIn = { Haptics.tap(context); mapViewRef.value?.controller?.zoomIn() },
                    onZoomOut = { Haptics.tap(context); mapViewRef.value?.controller?.zoomOut() }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Recenter
                RecenterButton(enabled = locationEnabled) {
                    Haptics.tap(context); userLocation?.let {
                        val z = repo.loadSetting("default_zoom", "15").toDoubleOrNull() ?: 15.0
                        mapViewRef.value?.controller?.animateTo(it, z, 600)
                    }
                    if (trackRecorder.isRecording) autoFollow = true
                }
            }
        }

        // Undo toast — spring animation entry/exit with timer progress bar
        androidx.compose.animation.AnimatedVisibility(
            visible = showUndoToast,
            enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 300f)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 300f)
                ) { it },
            exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 300f)
                ) + androidx.compose.animation.slideOutVertically(
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 300f)
                ) { it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp)
        ) {
            // Animate the timer progress from 1 to 0 over 5 seconds
            val timerAnimatable = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(Unit) {
                timerAnimatable.snapTo(1f)
                timerAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 5000, easing = androidx.compose.animation.core.LinearEasing))
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), shadowElevation = 6.dp) {
                Column {
                    Row(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Waypoint deleted", color = MaterialTheme.colorScheme.surface, fontSize = 14.sp, fontWeight = FontWeight.W500)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Undo", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.W600,
                            modifier = Modifier.iosClickable {
                                lastDeleted?.let { repo.restoreWaypoint(it); lastDeleted = null }
                                showUndoToast = false
                            }.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                    // Timer progress line
                    Box(
                        modifier = Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 4.dp).padding(bottom = 1.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width * timerAnimatable.value
                            drawLine(
                                color = Color(0xFF007AFF),
                                start = Offset(0f, size.height / 2),
                                end = Offset(barWidth, size.height / 2),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // Achievement banner
        androidx.compose.animation.AnimatedVisibility(
            visible = achievementBanner != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 80.dp)
        ) {
            achievementBanner?.let { text ->
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // ── Onboarding overlay (covers everything, edge-to-edge) ──
        if (showOnboarding) {
            OnboardingOverlay(onDone = {
                showOnboarding = false
                repo.saveSetting("has_onboarded", "true")
            })
        }
    }

    // ── Sheets & dialogs ────────────────────────────────────────
    pendingDelete?.let { wp ->
        DeleteConfirmSheet(waypointName = wp.name, onConfirm = {
            wp.photoPath?.let { File(it).delete() }
            repo.deleteWaypoint(wp.id); selectedWaypoint = null; lastDeleted = wp; pendingDelete = null
            showUndoToast = true
            scope.launch { kotlinx.coroutines.delay(5000); showUndoToast = false }
        }, onDismiss = { pendingDelete = null })
    }

    if (showEditSheet) {
        selectedWaypoint?.let { wp ->
            EditWaypointSheet(waypoint = wp, onDismiss = { showEditSheet = false },
                onSave = { name, notes, color, photoPath, icon ->
                    val s = name.trim().take(64).ifBlank { "Unnamed Waypoint" }
                    val updated = wp.copy(name = s, notes = notes.trim(), color = color, photoPath = photoPath, icon = icon)
                    repo.updateWaypoint(updated); selectedWaypoint = updated; showEditSheet = false; Haptics.tap(context)
                })
        }
    }

    if (showWaypointList) {
        WaypointListSheet(waypoints = waypoints, tracks = tracks, userLocation = userLocation,
            onSelectWaypoint = { wp -> selectedWaypoint = wp; mapViewRef.value?.controller?.animateTo(GeoPoint(wp.latitude, wp.longitude), 18.0, 800); showWaypointList = false },
            onSelectTrack = { track -> selectedTrack = track; if (track.points.isNotEmpty()) { val c = GeoPoint(track.points.sumOf { it.latitude } / track.points.size, track.points.sumOf { it.longitude } / track.points.size); mapViewRef.value?.controller?.animateTo(c, 15.0, 800) }; showWaypointList = false },
            onDeleteTrack = { track -> repo.deleteTrack(track.id) },
            onDeleteWaypoint = { wp -> pendingDelete = wp; showWaypointList = false },
            onDismiss = { showWaypointList = false })
    }

    selectedTrack?.let { track ->
        TrackDetailSheet(track = track, imperial = useImperial, onRename = { renamed ->
            repo.updateTrack(renamed); selectedTrack = renamed
        }, onDelete = { repo.deleteTrack(track.id); selectedTrack = null }, onDismiss = { selectedTrack = null })
    }

    if (showSettings) {
        SettingsSheet(
            store = repo.directStore,
            glareMode = glareMode,
            onToggleGlare = onToggleGlare,
            onDismiss = { showSettings = false; settingsVersion++ }
        )
    }

    if (showDailyStats) {
        DailyStatsSheet(
            store = repo.directStore, tracks = tracks, imperial = useImperial,
            onCompare = { showDailyStats = false; showCompare = true },
            onDismiss = { showDailyStats = false }
        )
    }

    if (showSpeedometer) {
        // Compute current-run vertical + count from active recording
        val currentStats = remember(trackRecorder.currentPoints.size) {
            computeTrackStats(trackRecorder.currentPoints)
        }
        SpeedometerSheet(
            currentSpeedKmh = currentSpeed * 3.6,
            altitudeMeters = currentAltitude,
            currentRunVertical = currentStats.verticalDescended,
            currentRunCount = currentStats.runCount,
            imperial = useImperial,
            onDismiss = { showSpeedometer = false }
        )
    }

    if (showCompare) {
        TrackCompareSheet(tracks = tracks, imperial = useImperial, onDismiss = { showCompare = false })
    }

    recapTrack?.let { t ->
        SessionRecapSheet(track = t, waypoints = waypoints, imperial = useImperial, onDismiss = { recapTrack = null })
    }

    if (showAwarenessSheet) {
        awareness?.let { a ->
            AvalancheAwarenessSheet(assessment = a, placeName = placeName, onDismiss = { showAwarenessSheet = false })
        }
    }

    if (showWeatherSheet) {
        WeatherForecastSheet(
            forecast = forecast,
            imperial = useImperial,
            placeName = placeName,
            onRefresh = {
                userLocation?.let { loc ->
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val f = fetchForecast(loc.latitude, loc.longitude)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { forecast = f }
                        }
                    }
                }
            },
            onDismiss = { showWeatherSheet = false }
        )
    }

    // Notification permission rationale dialog
    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text("Enable Notifications", fontWeight = FontWeight.W600) },
            text = { Text("Piste uses notifications to keep your run tracking active in the background. Without this, recording may stop when you lock your phone.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationale = false }) { Text("Not Now") }
            }
        )
    }

    // Battery optimization prompt
    if (showBatteryPrompt) {
        AlertDialog(
            onDismissRequest = { showBatteryPrompt = false },
            title = { Text("Disable Battery Optimization", fontWeight = FontWeight.W600) },
            text = { Text("Battery optimization can stop run tracking in the background. For reliable recording, allow Piste to run unrestricted.") },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryPrompt = false
                    repo.saveSetting("battery_prompt_shown", "true")
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatteryPrompt = false
                    repo.saveSetting("battery_prompt_shown", "true")
                }) { Text("Not Now") }
            }
        )
    }

    // Recording interrupted dialog
    if (showRecordingInterrupted) {
        AlertDialog(
            onDismissRequest = { showRecordingInterrupted = false },
            title = { Text("Recording Interrupted", fontWeight = FontWeight.W600) },
            text = { Text("A recording was in progress when the app was closed. Unfortunately the GPS data couldn't be saved.") },
            confirmButton = {
                TextButton(onClick = { showRecordingInterrupted = false }) { Text("OK") }
            }
        )
    }
}
