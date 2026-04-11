package com.florian.waypoint

/**
 * Bridge between the TrackingService (which receives notification-button taps
 * via [android.app.PendingIntent]) and the Compose-owned [TrackRecorder]
 * in MapScreen.
 *
 * MapScreen assigns the active recorder + stop callback when a recording starts,
 * clears them when it ends. The service reads them inside its intent handlers
 * to apply the user's notification action.
 *
 * Kept deliberately minimal — just two nullable fields. No thread synchronization
 * is needed because all mutations happen on the main thread.
 */
object RecordingBridge {
    var recorder: TrackRecorder? = null
    /** Called by the service when the user taps the "Stop" action in the notification. */
    var onStop: (() -> Unit)? = null
}
