package com.florian.waypoint

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Named haptic patterns that replace Android's flat single-shot `vibrate()` with
 * multi-step sequences intended to mimic iOS's UIFeedbackGenerator.
 *
 * Each named entry is mapped to a `VibrationEffect.createWaveform()` with
 * explicit timing and amplitude arrays so every event has its own "shape" —
 * a light-to-heavy crescendo for recording start, a heavy-tap-tap-tap for a
 * personal best, a double tap for a ring closing, etc.
 *
 * All calls are safe no-ops if the device lacks a vibrator.
 */
object Haptics {

    /** iOS-style light selection tap — used for button presses and tab switches. */
    fun tap(context: Context) {
        play(context, longArrayOf(0, 10), intArrayOf(0, 80))
    }

    /** iOS-style medium tap — used for confirmation actions (save, close). */
    fun mediumTap(context: Context) {
        play(context, longArrayOf(0, 15), intArrayOf(0, 150))
    }

    /** iOS "pop" feel — used for long-press activation and context menus. */
    fun pop(context: Context) {
        play(context, longArrayOf(0, 8, 30, 12), intArrayOf(0, 200, 0, 120))
    }

    /** Crescendo pattern for starting a recording — "something big is beginning". */
    fun recordStart(context: Context) {
        play(context, longArrayOf(0, 20, 40, 30), intArrayOf(0, 100, 0, 200))
    }

    /** Descending release pattern for stopping a recording — "we're done". */
    fun recordStop(context: Context) {
        play(context, longArrayOf(0, 25, 30, 15, 25, 10), intArrayOf(0, 200, 0, 120, 0, 60))
    }

    /** Celebration pattern for a new personal best — single heavy tap, pause, triple flick. */
    fun personalBest(context: Context) {
        play(
            context,
            longArrayOf(0, 40, 80, 15, 40, 15, 40, 15),
            intArrayOf(0, 255, 0, 180, 0, 180, 0, 180)
        )
    }

    /** Double tap for an activity ring closing — short, satisfying. */
    fun ringClosed(context: Context) {
        play(context, longArrayOf(0, 18, 40, 22), intArrayOf(0, 220, 0, 255))
    }

    /** Error / warning — a short double buzz. */
    fun warning(context: Context) {
        play(context, longArrayOf(0, 30, 80, 30), intArrayOf(0, 180, 0, 180))
    }

    /** Proximity alert — matches the old vibrate() but with a shaped pattern. */
    fun proximity(context: Context) {
        play(context, longArrayOf(0, 20, 50, 20), intArrayOf(0, 160, 0, 160))
    }

    // ── Internal plumbing ───────────────────────────────────────
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun play(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } catch (_: Exception) {
            // Fall back to a flat one-shot if the waveform API chokes on this device.
            try {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            } catch (_: Exception) {
            }
        }
    }
}
