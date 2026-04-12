package com.florian.piste

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import org.osmdroid.util.GeoPoint
import kotlin.math.*

fun createPinDrawable(context: Context, color: Color, label: String, iconKey: String? = null): BitmapDrawable {
    val d = context.resources.displayMetrics.density

    // Dimensions
    val headR = 14f * d
    val pinH = 38f * d
    val dotR = 5f * d
    val gap = 4f * d

    // ── Label ───────────────────────────────────────────────────
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(200, 30, 30, 34)
        textSize = 10f * d
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val truncated = if (label.length > 14) label.take(13) + "\u2026" else label
    val textW = textPaint.measureText(truncated)
    val ph = 7f * d; val pv = 3.5f * d
    val pillW = textW + ph * 2
    val pillH = textPaint.textSize + pv * 2
    val pillR = pillH / 2f

    // Canvas
    val w = maxOf(pillW, headR * 2) + 10f * d
    val h = pillH + gap + pinH + 6f * d
    val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = w / 2f

    // ── Label pill ──────────────────────────────────────────────
    val pillRect = RectF(cx - pillW / 2, 0f, cx + pillW / 2, pillH)
    // Shadow
    canvas.drawRoundRect(
        pillRect.left + 0.5f * d, pillRect.top + 1f * d, pillRect.right + 0.5f * d, pillRect.bottom + 1f * d,
        pillR, pillR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb(20, 0, 0, 0); style = Paint.Style.FILL
        }
    )
    // Fill
    canvas.drawRoundRect(pillRect, pillR, pillR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(242, 255, 255, 255); style = Paint.Style.FILL
    })
    // Text
    canvas.drawText(truncated, cx, pv + textPaint.textSize - textPaint.descent(), textPaint)

    // ── Pin ─────────────────────────────────────────────────────
    val pinTop = pillH + gap
    val headCy = pinTop + headR
    val tipY = pinTop + pinH

    val argb = android.graphics.Color.argb(
        (color.alpha * 255).toInt(), (color.red * 255).toInt(),
        (color.green * 255).toInt(), (color.blue * 255).toInt()
    )

    // Simple teardrop: 260° arc + straight lines to tip
    val path = Path().apply {
        arcTo(cx - headR, headCy - headR, cx + headR, headCy + headR, 140f, 260f, true)
        lineTo(cx, tipY)
        close()
    }

    // Shadow (offset copy)
    val shadowPath = Path(path)
    shadowPath.offset(0f, 1.5f * d)
    canvas.drawPath(shadowPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(35, 0, 0, 0); style = Paint.Style.FILL
    })

    // Fill
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = argb; style = Paint.Style.FILL
    })

    // ── Inner dot or icon ───────────────────────────────────────
    val symbol = iconSymbol(iconKey)
    if (symbol != null) {
        canvas.drawText(symbol, cx, headCy + 3.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = 10f * d; textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
    } else {
        canvas.drawCircle(cx, headCy, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb(255, 255, 255, 255)
        })
    }

    return BitmapDrawable(context.resources, bmp)
}

fun createUserDotDrawable(context: Context): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (36 * d).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    canvas.drawCircle(cx, cx, 18 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(30, 0, 122, 255)
    })
    canvas.drawCircle(cx, cx, 11 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })
    canvas.drawCircle(cx, cx, 8 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 0, 122, 255)
    })
    return BitmapDrawable(context.resources, bmp)
}

/** Safe color parse — returns default charcoal on invalid hex. */
fun safeParseColor(hex: String?): Int =
    try { android.graphics.Color.parseColor(hex ?: "#3C3734") } catch (_: Exception) { android.graphics.Color.parseColor("#3C3734") }

/** Bearing in degrees (0-360, north=0) from point a to point b. */
fun bearingDegrees(a: GeoPoint, b: GeoPoint): Float {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}

fun formatDistance(meters: Double, imperial: Boolean = false): String =
    if (imperial) {
        val feet = meters * 3.28084
        if (feet < 5280) "${feet.toInt()} ft away" else "%.1f mi away".format(feet / 5280)
    } else {
        if (meters < 1000) "${meters.toInt()} m away" else "%.1f km away".format(meters / 1000)
    }

fun formatSpeed(speedMps: Float, imperial: Boolean = false): String? {
    if (speedMps < 0.3f) return null // standing still
    return if (imperial) "%.1f mph".format(speedMps * 2.23694)
    else "%.1f km/h".format(speedMps * 3.6)
}


fun vibrate(context: Context, light: Boolean = false) {
    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    val effect = if (light)
        VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
    else
        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
    vibrator.vibrate(effect)
}
