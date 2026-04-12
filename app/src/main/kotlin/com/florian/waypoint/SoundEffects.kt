package com.florian.waypoint

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Subtle interaction sounds layered on top of the haptic feedback.
 *
 * Uses [ToneGenerator] so we don't need to ship any audio assets. The tone
 * categories are a bit coarse — we pick the built-in Android tones that most
 * closely resemble iOS sound semantics (rising note = action start, descending
 * note = action stop, bell = celebration, tock = light press).
 *
 * Every entry respects the "sound_enabled" user setting and fails silently
 * if the audio system is unavailable. Volume is kept deliberately quiet
 * (40/100) so sounds feel like polish, not like alerts.
 */
object SoundEffects {

    private const val VOLUME = 40

    /** Returns true if the user has enabled sound in settings. */
    private fun isEnabled(store: WaypointStore): Boolean =
        store.loadSetting("sound_enabled", "true") == "true"

    /** Rising confirmation — used when a recording session starts. */
    fun recordStart(context: Context, store: WaypointStore) {
        if (!isEnabled(store)) return
        play(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    /** Short descending tone — used when a recording session stops. */
    fun recordStop(context: Context, store: WaypointStore) {
        if (!isEnabled(store)) return
        play(ToneGenerator.TONE_PROP_ACK, 180)
    }

    /** Soft bell — used for personal-best achievements. */
    fun personalBest(context: Context, store: WaypointStore) {
        if (!isEnabled(store)) return
        play(ToneGenerator.TONE_PROP_BEEP2, 250)
    }

    /** Tock / pop — used for long-press and context-menu activation. */
    fun pop(context: Context, store: WaypointStore) {
        if (!isEnabled(store)) return
        play(ToneGenerator.TONE_PROP_PROMPT, 80)
    }

    /** Two-note "ring closed" chime for activity rings. */
    fun ringClosed(context: Context, store: WaypointStore) {
        if (!isEnabled(store)) return
        play(ToneGenerator.TONE_PROP_ACK, 120)
    }

    private fun play(toneType: Int, durationMs: Int) {
        try {
            // Each call builds a fresh generator and releases it after playback. This is
            // slightly wasteful but avoids needing a persistent audio resource and keeps
            // the tone from ever playing for longer than expected if we get killed.
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
            tg.startTone(toneType, durationMs)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { tg.release() } catch (_: Exception) {}
            }, (durationMs + 50).toLong())
        } catch (_: Exception) {
            // Silently ignore — sound is polish, never required.
        }
    }
}
