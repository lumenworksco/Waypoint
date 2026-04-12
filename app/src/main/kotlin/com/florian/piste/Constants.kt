package com.florian.piste

object PisteConstants {
    // Track analysis
    const val MIN_SPEED_MS = 2.0          // Lower bound filters lift rides
    const val MAX_SPEED_MS = 40.0         // Upper bound filters GPS jumps (144 km/h)
    const val MIN_RUN_VERTICAL_M = 30.0   // Minimum vertical drop to count as a run
    const val DESCENT_THRESHOLD = -0.5    // Altitude change to start descent detection
    const val CLIMB_THRESHOLD = 1.0       // Altitude change to end a run
    const val SPEED_HEATMAP_MAX_KMH = 80.0
    const val EASY_GRADIENT = 0.15
    const val MODERATE_GRADIENT = 0.30
    const val RUN_MIN_SPEED_KMH = 7.0    // Filter lift speeds in run breakdown
    const val RUN_MAX_SPEED_KMH = 144.0  // Filter GPS jumps in run breakdown

    // Air time detection
    const val AIR_GRAVITY_THRESHOLD = 2.5f   // m/s², below = airborne
    const val AIR_MIN_DURATION_MS = 500L
    const val AIR_MAX_DURATION_MS = 10_000L

    // Location
    const val LOCATION_INTERVAL_MS = 3000L
    const val LOCATION_MIN_DISTANCE_M = 2f
    const val PLACE_REFETCH_DISTANCE_M = 5000.0
    const val PROXIMITY_THROTTLE_MS = 15_000L

    // Weather & data refresh
    const val WEATHER_REFRESH_MS = 15 * 60_000L
    const val FORECAST_REFRESH_MS = 30 * 60_000L
    const val DAYLIGHT_REFRESH_MS = 60_000L
    const val DAYLIGHT_WARN_MINUTES = 90
    const val DAYLIGHT_URGENT_MINUTES = 30

    // Tile cache
    const val TILE_CACHE_MAX_BYTES = 100L * 1024 * 1024
    const val TILE_CACHE_TRIM_BYTES = 80L * 1024 * 1024

    // Recording
    const val AUTO_RECAP_DURATION_MS = 45 * 60_000L
    const val ACHIEVEMENT_DISPLAY_MS = 4000L
    const val UNDO_DISPLAY_MS = 5000L

    // Speed heatmap
    const val HEATMAP_BATCH_SIZE = 8

    // Achievements thresholds
    const val PB_MIN_SPEED_KMH = 5.0
    const val PB_MIN_VERTICAL_M = 100.0
    const val PB_MIN_DISTANCE_M = 100.0

    // Avalanche awareness thresholds
    const val AVALANCHE_SNOW_THRESHOLD_CM = 20.0
    const val AVALANCHE_WIND_THRESHOLD_KMH = 40.0
    const val AVALANCHE_WIND_SNOW_THRESHOLD_CM = 5.0
    const val AVALANCHE_TEMP_SWING_THRESHOLD_C = 10.0

    // UI
    const val WAYPOINT_NAME_MAX_LENGTH = 50
    const val WAYPOINT_NOTES_MAX_LENGTH = 500
    const val DEFAULT_ZOOM = 15.0
    const val MIN_ZOOM = 3.0
    const val MAX_ZOOM = 20.0

    // Cluster
    const val CLUSTER_MIN_COUNT = 3
    const val CLUSTER_MAX_ZOOM = 15
    const val CLUSTER_CELL_DP = 60

    // Nominatim rate limit
    const val NOMINATIM_TIMEOUT_MS = 5000
}
