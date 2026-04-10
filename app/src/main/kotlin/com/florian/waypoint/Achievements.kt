package com.florian.waypoint

data class PersonalBests(
    val maxSpeedKmh: Double,
    val maxDayVertical: Double,
    val maxDayRuns: Int,
    val maxDayDistance: Double
)

fun WaypointStore.loadPersonalBests(): PersonalBests = PersonalBests(
    maxSpeedKmh = loadSetting("pb_max_speed", "0").toDoubleOrNull() ?: 0.0,
    maxDayVertical = loadSetting("pb_max_vertical", "0").toDoubleOrNull() ?: 0.0,
    maxDayRuns = loadSetting("pb_max_runs", "0").toIntOrNull() ?: 0,
    maxDayDistance = loadSetting("pb_max_distance", "0").toDoubleOrNull() ?: 0.0
)

fun WaypointStore.savePersonalBests(pb: PersonalBests) {
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
    val pb = loadPersonalBests()
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

    if (updated != pb) savePersonalBests(updated)
    return achievements
}
