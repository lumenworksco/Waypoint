package com.florian.waypoint

import java.util.UUID

data class Waypoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
