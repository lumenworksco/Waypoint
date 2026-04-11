package com.florian.waypoint

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground service that keeps the GPS awake while a track is being recorded.
 *
 * The notification shown to the user has Pause/Resume and Stop action buttons
 * that route back to this service via [PendingIntent.getService]. When the
 * user taps them, the service mutates the [TrackRecorder] through
 * [RecordingBridge] and rebuilds the notification so the button labels stay
 * in sync with the current state.
 *
 * A 1-second heartbeat keeps the notification's elapsed-time counter live
 * while recording is active (notification time builder alone is enough for
 * "chronometer" mode, but on some OEMs the counter freezes if we never
 * re-post, so we refresh it periodically).
 */
class TrackingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> RecordingBridge.recorder?.pause()
            ACTION_RESUME -> RecordingBridge.recorder?.resume()
            ACTION_STOP -> {
                // Hand off the actual stop work (saving track, showing recap) to MapScreen
                RecordingBridge.onStop?.invoke()
                // onStop typically calls stopService — but if it didn't, make sure we exit
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        ensureChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 30_000)
        return START_STICKY
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Track Recording", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Active while recording a track"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(): android.app.Notification {
        val paused = RecordingBridge.recorder?.isPaused == true

        // Tap body → open the app
        val tapIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPending = PendingIntent.getActivity(this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE)

        // Pause / Resume action
        val pauseResumeAction = if (paused) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Resume",
                servicePendingIntent(ACTION_RESUME, REQ_RESUME)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Pause",
                servicePendingIntent(ACTION_PAUSE, REQ_PAUSE)
            ).build()
        }

        // Stop action
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel, "Stop",
            servicePendingIntent(ACTION_STOP, REQ_STOP)
        ).build()

        val title = if (paused) "Recording paused" else "Recording your track\u2026"
        val text = RecordingBridge.recorder?.currentPoints?.size
            ?.takeIf { it > 0 }
            ?.let { "$it points captured" }
            ?: "Waiting for GPS fix"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tapPending)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TrackingService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "waypoint_tracking"
        private const val NOTIFICATION_ID = 1
        private const val REQ_PAUSE = 101
        private const val REQ_RESUME = 102
        private const val REQ_STOP = 103

        const val ACTION_PAUSE = "com.florian.waypoint.action.PAUSE"
        const val ACTION_RESUME = "com.florian.waypoint.action.RESUME"
        const val ACTION_STOP = "com.florian.waypoint.action.STOP"

        /**
         * Ask an already-running service to rebuild its notification, e.g. after the
         * in-app pause/resume button is tapped. Safe to call when the service isn't
         * running — the system will simply ignore the intent.
         */
        fun refresh(context: Context) {
            try {
                context.startService(Intent(context, TrackingService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
