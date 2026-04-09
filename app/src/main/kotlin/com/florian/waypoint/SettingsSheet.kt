package com.florian.waypoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DistanceUnit(val label: String) { METRIC("Metric (km)"), IMPERIAL("Imperial (mi)") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(store: WaypointStore, onDismiss: () -> Unit) {
    var unit by remember { mutableStateOf(
        runCatching { DistanceUnit.valueOf(store.loadSetting("distance_unit", "METRIC")) }.getOrDefault(DistanceUnit.METRIC)
    ) }
    var defaultZoom by remember { mutableFloatStateOf(
        store.loadSetting("default_zoom", "15").toFloatOrNull() ?: 15f
    ) }
    var proximityRadius by remember { mutableIntStateOf(
        store.loadSetting("proximity_radius", "0").toIntOrNull() ?: 0
    ) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
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
            SettingLabel("Default Zoom Level: ${defaultZoom.toInt()}")
            Slider(
                value = defaultZoom,
                onValueChange = { defaultZoom = it; store.saveSetting("default_zoom", it.toInt().toString()) },
                valueRange = 5f..19f,
                steps = 13,
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
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
    Spacer(modifier = Modifier.height(8.dp))
}
