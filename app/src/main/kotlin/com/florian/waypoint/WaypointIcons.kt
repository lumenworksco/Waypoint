package com.florian.waypoint

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class PresetIcon(val key: String, val label: String, val icon: ImageVector, val symbol: String)

/** Ski-themed preset icons. Symbol is rendered inside the pin head. */
val WaypointPresetIcons = listOf(
    PresetIcon("pin", "Pin", Icons.Filled.Place, "\u25CF"),
    PresetIcon("lift", "Lift", Icons.Filled.Cable, "L"),
    PresetIcon("gondola", "Gondola", Icons.Filled.DirectionsBoat, "G"),
    PresetIcon("cabin", "Hut", Icons.Filled.Cabin, "H"),
    PresetIcon("restaurant", "Food", Icons.Filled.Restaurant, "\u2616"),
    PresetIcon("firstaid", "First Aid", Icons.Filled.LocalHospital, "+"),
    PresetIcon("powder", "Powder", Icons.Filled.AcUnit, "\u2744"),
    PresetIcon("cliff", "Cliff", Icons.Filled.Terrain, "\u25B2"),
    PresetIcon("tree", "Trees", Icons.Filled.Park, "\u2663"),
    PresetIcon("warning", "Warning", Icons.Filled.Warning, "!"),
    PresetIcon("star", "Favorite", Icons.Filled.Star, "\u2605"),
    PresetIcon("flag", "Start", Icons.Filled.Flag, "\u2691"),
)

fun iconSymbol(key: String?): String? = key?.let { k ->
    WaypointPresetIcons.find { it.key == k }?.symbol
}
