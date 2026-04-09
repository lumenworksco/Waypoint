package com.florian.waypoint

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class PresetIcon(val key: String, val label: String, val icon: ImageVector, val symbol: String)

val WaypointPresetIcons = listOf(
    PresetIcon("pin", "Pin", Icons.Filled.Place, "\u25CF"),
    PresetIcon("parking", "Parking", Icons.Filled.LocalParking, "P"),
    PresetIcon("park", "Park", Icons.Filled.Park, "\u2663"),
    PresetIcon("restaurant", "Food", Icons.Filled.Restaurant, "\u2616"),
    PresetIcon("hotel", "Hotel", Icons.Filled.Hotel, "H"),
    PresetIcon("gas", "Gas", Icons.Filled.LocalGasStation, "G"),
    PresetIcon("water", "Water", Icons.Filled.Water, "\u223F"),
    PresetIcon("viewpoint", "View", Icons.Filled.Landscape, "\u25B2"),
    PresetIcon("warning", "Warning", Icons.Filled.Warning, "!"),
    PresetIcon("star", "Star", Icons.Filled.Star, "\u2605"),
    PresetIcon("home", "Home", Icons.Filled.Home, "\u2302"),
    PresetIcon("info", "Info", Icons.Filled.Info, "i"),
)

fun iconSymbol(key: String?): String? = key?.let { k ->
    WaypointPresetIcons.find { it.key == k }?.symbol
}
