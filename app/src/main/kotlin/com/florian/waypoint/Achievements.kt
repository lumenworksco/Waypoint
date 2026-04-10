package com.florian.waypoint

data class PersonalBests(
    val maxSpeedKmh: Double,
    val maxDayVertical: Double,
    val maxDayRuns: Int,
    val maxDayDistance: Double
)

/**
 * Compute personal bests directly from the full track history.
 * This is always accurate, even for imported GPX tracks.
 */
fun computePersonalBests(tracks: List<Track>): PersonalBests {
    if (tracks.isEmpty()) return PersonalBests(0.0, 0.0, 0, 0.0)

    // Max speed ever
    var maxSpeed = 0.0
    for (t in tracks) {
        val s = computeTrackStats(t.points)
        if (s.maxSpeedKmh > maxSpeed) maxSpeed = s.maxSpeedKmh
    }

    // Group tracks by day and find the best day
    val dayGroups = mutableMapOf<Long, MutableList<Track>>()
    for (t in tracks) {
        dayGroups.getOrPut(startOfDay(t.startTime)) { mutableListOf() }.add(t)
    }

    var maxDayVert = 0.0
    var maxDayRuns = 0
    var maxDayDist = 0.0
    for ((_, dayTracks) in dayGroups) {
        var v = 0.0; var r = 0; var d = 0.0
        for (t in dayTracks) {
            val s = computeTrackStats(t.points)
            v += s.verticalDescended ?: 0.0
            r += s.runCount
            d += s.distanceMeters
        }
        if (v > maxDayVert) maxDayVert = v
        if (r > maxDayRuns) maxDayRuns = r
        if (d > maxDayDist) maxDayDist = d
    }

    return PersonalBests(maxSpeed, maxDayVert, maxDayRuns, maxDayDist)
}

// Legacy stored-bests accessors kept for achievement-change detection only.
fun WaypointStore.loadStoredBests(): PersonalBests = PersonalBests(
    maxSpeedKmh = loadSetting("pb_max_speed", "0").toDoubleOrNull() ?: 0.0,
    maxDayVertical = loadSetting("pb_max_vertical", "0").toDoubleOrNull() ?: 0.0,
    maxDayRuns = loadSetting("pb_max_runs", "0").toIntOrNull() ?: 0,
    maxDayDistance = loadSetting("pb_max_distance", "0").toDoubleOrNull() ?: 0.0
)

fun WaypointStore.saveStoredBests(pb: PersonalBests) {
    saveSetting("pb_max_speed", pb.maxSpeedKmh.toString())
    saveSetting("pb_max_vertical", pb.maxDayVertical.toString())
    saveSetting("pb_max_runs", pb.maxDayRuns.toString())
    saveSetting("pb_max_distance", pb.maxDayDistance.toString())
}

/**
 * Compare current totals against stored personal bests.
 * Returns a list of achievement strings for any records broken.
 * Mutates the store with new bests.
 */
fun WaypointStore.checkAchievements(
    newMaxSpeedKmh: Double,
    todayVertical: Double,
    todayRuns: Int,
    todayDistance: Double
): List<String> {
    val pb = loadStoredBests()
    val achievements = mutableListOf<String>()
    var updated = pb

    if (newMaxSpeedKmh > pb.maxSpeedKmh && newMaxSpeedKmh > 5) {
        achievements.add("\uD83C\uDFC1 New top speed: ${formatTrackSpeed(newMaxSpeedKmh)}")
        updated = updated.copy(maxSpeedKmh = newMaxSpeedKmh)
    }
    if (todayVertical > pb.maxDayVertical && todayVertical > 100) {
        achievements.add("\uD83C\uDFC6 New daily vertical record!")
        updated = updated.copy(maxDayVertical = todayVertical)
    }
    if (todayRuns > pb.maxDayRuns && todayRuns > 0) {
        achievements.add("\uD83C\uDFAF Most runs in a day: $todayRuns")
        updated = updated.copy(maxDayRuns = todayRuns)
    }
    if (todayDistance > pb.maxDayDistance && todayDistance > 100) {
        updated = updated.copy(maxDayDistance = todayDistance)
    }

    if (updated != pb) saveStoredBests(updated)
    return achievements
}
