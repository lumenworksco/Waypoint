package com.florian.waypoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.config.Configuration
import java.io.File

enum class DistanceUnit(val label: String) { METRIC("Metric (km)"), IMPERIAL("Imperial (mi)") }

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
    var cacheSize by remember { mutableStateOf(calcCacheSize(context)) }
    var keepScreenOn by remember { mutableStateOf(store.loadSetting("keep_screen_on", "true") == "true") }

    // Easter egg state
    var logoTapCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Settings", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
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
                            unit = u; store.saveSetting("distance_unit", u.name)
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

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Glare mode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Glare Mode", fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Text("High-contrast UI for bright snow", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = glareMode, onCheckedChange = { onToggleGlare() })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keep screen on
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep Screen On", fontSize = 15.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
                    Text("Stay awake while recording a track", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = keepScreenOn, onCheckedChange = {
                    keepScreenOn = it
                    store.saveSetting("keep_screen_on", it.toString())
                })
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
                        clearTileCache(context)
                        cacheSize = calcCacheSize(context)
                    }.padding(horizontal = 8.dp, vertical = 6.dp))
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
                Text("Version 1.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
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
                    Text("\uD83D\uDDFA\uFE0F", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Formerly known as Waypoint",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W700,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This app began as a simple waypoint tracker on OpenStreetMap, " +
                        "built somewhere in the Alps with too much free time and a love for fresh powder. " +
                        "Thanks for being here since the beginning.",
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
                                "See you on the mountain",
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

private fun calcCacheSize(context: android.content.Context): String {
    val cacheDir = Configuration.getInstance().osmdroidTileCache ?: File(context.cacheDir, "osmdroid/tiles")
    if (!cacheDir.exists()) return "No cached tiles"
    val bytes = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}

private fun clearTileCache(context: android.content.Context) {
    val cacheDir = Configuration.getInstance().osmdroidTileCache ?: File(context.cacheDir, "osmdroid/tiles")
    if (cacheDir.exists()) cacheDir.deleteRecursively()
    // Also clear the SQL cache
    val sqlDb = File(context.filesDir?.parentFile, "databases")
    sqlDb.listFiles()?.filter { it.name.startsWith("tile") }?.forEach { it.delete() }
}
