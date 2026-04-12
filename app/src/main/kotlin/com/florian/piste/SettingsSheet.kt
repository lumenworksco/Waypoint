package com.florian.piste

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DistanceUnit(val label: String) { METRIC("Metric (km)"), IMPERIAL("Imperial (mi)") }

/**
 * Bottom sheet containing user-adjustable settings: distance units, default zoom,
 * proximity alert radius, glare mode, keep-screen-on, and offline tile cache
 * management. Also contains the About section with an easter-egg tap counter
 * (tap the "Piste" title 7 times to reveal the legacy Waypoint dialog).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(store: WaypointStore, glareMode: Boolean, onToggleGlare: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var unit by remember { mutableStateOf(
        runCatching { DistanceUnit.valueOf(store.loadSetting("distance_unit", "METRIC")) }.getOrDefault(DistanceUnit.METRIC)
    ) }
    var defaultZoom by remember { mutableFloatStateOf(
        store.loadSetting("default_zoom", "15").toFloatOrNull() ?: 15f
    ) }
    var proximityRadius by remember { mutableIntStateOf(
        store.loadSetting("proximity_radius", "0").toIntOrNull() ?: 0
    ) }
    var cacheSize by remember { mutableStateOf(TileCacheManager.cacheSize(context)) }
    var keepScreenOn by remember { mutableStateOf(store.loadSetting("keep_screen_on", "true") == "true") }
    var autoDarkMap by remember { mutableStateOf(store.loadSetting("auto_dark_map", "true") == "true") }
    var airTimeEnabled by remember { mutableStateOf(store.loadSetting("air_time_enabled", "true") == "true") }
    var widgetMode by remember { mutableStateOf(store.loadSetting("widget_mode", "Today")) }
    var verticalGoal by remember { mutableFloatStateOf(
        store.loadSetting("goal_vertical_m", "3000").toFloatOrNull() ?: 3000f
    ) }
    var runsGoal by remember { mutableFloatStateOf(
        store.loadSetting("goal_runs", "10").toFloatOrNull() ?: 10f
    ) }
    var timeGoalMin by remember { mutableFloatStateOf(
        store.loadSetting("goal_time_min", "240").toFloatOrNull() ?: 240f
    ) }

    // Easter egg state
    var logoTapCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue != SheetValue.Hidden) {
            Haptics.tap(context)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize().iosClickable(onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Distance unit
            SettingLabel("Distance Unit")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DistanceUnit.entries.forEach { u ->
                    val selected = u == unit
                    Surface(
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(modifier = Modifier.fillMaxSize().iosClickable {
                            Haptics.tap(context); unit = u; store.saveSetting("distance_unit", u.name)
                        }, contentAlignment = Alignment.Center) {
                            Text(u.label, fontSize = 14.sp, fontWeight = FontWeight.W500,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Default zoom
            SettingLabel("Default Zoom: ${defaultZoom.toInt()}")
            Slider(
                value = defaultZoom,
                onValueChange = { defaultZoom = it; store.saveSetting("default_zoom", it.toInt().toString()) },
                valueRange = 10f..19f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Proximity alert radius
            SettingLabel("Proximity Alert: ${if (proximityRadius == 0) "Off" else "${proximityRadius}m"}")
            Slider(
                value = proximityRadius.toFloat(),
                onValueChange = {
                    proximityRadius = it.toInt()
                    store.saveSetting("proximity_radius", it.toInt().toString())
                },
                valueRange = 0f..500f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (proximityRadius == 0) "No alerts" else "Vibrate when within ${proximityRadius}m of a waypoint",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Speed alert
            var speedAlert by remember { mutableFloatStateOf(
                store.loadSetting("speed_alert_kmh", "0").toFloatOrNull() ?: 0f
            ) }
            SettingLabel("Speed Alert: ${if (speedAlert.toInt() == 0) "Off" else "${speedAlert.toInt()} km/h"}")
            Slider(
                value = speedAlert,
                onValueChange = {
                    speedAlert = it
                    store.saveSetting("speed_alert_kmh", it.toInt().toString())
                },
                valueRange = 0f..120f,
                steps = 23,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (speedAlert.toInt() == 0) "No speed alerts" else "Vibrate when you exceed ${speedAlert.toInt()} km/h",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Daily goals (activity rings)
            Text("Daily Goals", fontSize = 13.sp, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Set the three activity rings you'll try to close every day on the mountain.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(14.dp))

            SettingLabel("Vertical Goal: ${verticalGoal.toInt()} m")
            Slider(
                value = verticalGoal,
                onValueChange = { verticalGoal = it; store.saveSetting("goal_vertical_m", it.toInt().toString()) },
                valueRange = 500f..10_000f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingLabel("Runs Goal: ${runsGoal.toInt()} runs")
            Slider(
                value = runsGoal,
                onValueChange = { runsGoal = it; store.saveSetting("goal_runs", it.toInt().toString()) },
                valueRange = 3f..30f,
                steps = 26,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingLabel("Time Goal: ${(timeGoalMin / 60).toInt()}h ${(timeGoalMin % 60).toInt()}m")
            Slider(
                value = timeGoalMin,
                onValueChange = { timeGoalMin = it; store.saveSetting("goal_time_min", it.toInt().toString()) },
                valueRange = 60f..480f,
                steps = 13,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Glare mode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Glare Mode", fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Text("High-contrast UI for bright snow", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = glareMode, onCheckedChange = { Haptics.tap(context); onToggleGlare() },
                    modifier = Modifier.semantics { contentDescription = "Toggle Glare Mode" })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keep screen on
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep Screen On", fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Text("Stay awake while recording a track", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = keepScreenOn, onCheckedChange = {
                    Haptics.tap(context); keepScreenOn = it
                    store.saveSetting("keep_screen_on", it.toString())
                }, modifier = Modifier.semantics { contentDescription = "Toggle Keep Screen On" })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto dark map
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Dark Map", fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Text("Switch to dark map tiles when system dark mode is active", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = autoDarkMap, onCheckedChange = {
                    Haptics.tap(context); autoDarkMap = it
                    store.saveSetting("auto_dark_map", it.toString())
                }, modifier = Modifier.semantics { contentDescription = "Toggle Auto Dark Map" })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Air time detection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Air Time Detection", fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Text("Detect airtime using your phone's accelerometer. May be inaccurate in bumpy terrain.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = airTimeEnabled, onCheckedChange = {
                    Haptics.tap(context); airTimeEnabled = it
                    store.saveSetting("air_time_enabled", it.toString())
                }, modifier = Modifier.semantics { contentDescription = "Toggle Air Time Detection" })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Widget display mode
            SettingLabel("Widget Display")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Today", "Season").forEach { mode ->
                    val selected = mode == widgetMode
                    Surface(
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(modifier = Modifier.fillMaxSize().iosClickable {
                            Haptics.tap(context); widgetMode = mode; store.saveSetting("widget_mode", mode)
                            StatsWidget.refreshAll(context)
                        }, contentAlignment = Alignment.Center) {
                            Text(mode, fontSize = 14.sp, fontWeight = FontWeight.W500,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Offline tiles management
            SettingLabel("Offline Map Tiles")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cacheSize, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("Clear cache", fontSize = 14.sp, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.iosClickable {
                        TileCacheManager.clearCache(context)
                        cacheSize = TileCacheManager.cacheSize(context)
                    }.padding(horizontal = 8.dp, vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // ── About ───────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Piste",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.iosClickable {
                        logoTapCount++
                        if (logoTapCount >= 7) {
                            showEasterEgg = true
                            logoTapCount = 0
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Version 2.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "\u26F7\uFE0F  Crafted in the French Alps",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ── Easter egg dialog (rendered in its own window layer) ────
    if (showEasterEgg) {
        val compassRotation = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            compassRotation.animateTo(360f, animationSpec = tween(durationMillis = 2000))
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEasterEgg = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "\uD83E\uDDED",
                        fontSize = 56.sp,
                        modifier = Modifier.rotate(compassRotation.value)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "The Waypoint Era",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W700,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Before the pistes called, this was Waypoint \u2014 a scrappy map app born from " +
                        "a love of mountains and a need to never forget where the best powder was. " +
                        "Every waypoint you drop carries that spirit forward.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().iosClickable { showEasterEgg = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Drop a pin for old times \uD83D\uDCCD",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.W600,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
    Spacer(modifier = Modifier.height(8.dp))
}
