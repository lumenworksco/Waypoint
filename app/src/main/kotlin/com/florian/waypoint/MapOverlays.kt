package com.florian.waypoint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import org.osmdroid.util.GeoPoint
import kotlin.math.*

fun createPinDrawable(context: Context, color: Color, label: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val headR = 13 * density
    val pinH = 38 * density
    val innerR = 5 * density
    val labelGap = 3 * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(230, 28, 28, 30)
        textSize = 10 * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val truncated = if (label.length > 12) label.take(11) + "\u2026" else label
    val textW = textPaint.measureText(truncated)
    val labelPadH = 6 * density
    val labelPadV = 3 * density
    val labelW = textW + labelPadH * 2
    val labelH = textPaint.textSize + labelPadV * 2

    val totalW = maxOf(labelW, headR * 2) + 6 * density
    val totalH = labelH + labelGap + pinH + 2 * density
    val bmp = Bitmap.createBitmap(totalW.toInt(), totalH.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = totalW / 2f

    // Label pill
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(245, 255, 255, 255)
        style = Paint.Style.FILL
        setShadowLayer(2 * density, 0f, 0.5f * density, android.graphics.Color.argb(40, 0, 0, 0))
    }
    canvas.drawRoundRect(cx - labelW / 2, 0f, cx + labelW / 2, labelH, 6 * density, 6 * density, bgPaint)
    canvas.drawText(truncated, cx, labelPadV + textPaint.textSize - textPaint.descent(), textPaint)

    // Pin shape
    val pinTop = labelH + labelGap
    val headCy = pinTop + headR
    val tipY = pinTop + pinH
    val argb = android.graphics.Color.argb(
        (color.alpha * 255).toInt(), (color.red * 255).toInt(),
        (color.green * 255).toInt(), (color.blue * 255).toInt()
    )
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = argb
        style = Paint.Style.FILL
        setShadowLayer(3 * density, 0f, 1.5f * density, android.graphics.Color.argb(60, 0, 0, 0))
    }
    val path = Path().apply {
        arcTo(cx - headR, headCy - headR, cx + headR, headCy + headR, 145f, 250f, true)
        lineTo(cx, tipY)
        close()
    }
    canvas.drawPath(path, pinPaint)

    // White inner dot
    canvas.drawCircle(cx, headCy, innerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
    })

    return BitmapDrawable(context.resources, bmp)
}

fun createUserDotDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    canvas.drawCircle(cx, cx, 18 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(36, 0, 122, 255)
    })
    canvas.drawCircle(cx, cx, 11 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })
    canvas.drawCircle(cx, cx, 8 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 0, 122, 255)
    })
    return BitmapDrawable(context.resources, bmp)
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

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m away"
    else "%.1f km away".format(meters / 1000)

fun vibrate(context: Context, light: Boolean = false) {
    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    val effect = if (light)
        VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
    else
        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
    vibrator.vibrate(effect)
}
