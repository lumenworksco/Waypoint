package com.florian.piste

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Render a shareable stats card for a track and launch the system share sheet.
 */
fun shareTrackImage(context: Context, track: Track, imperial: Boolean) {
    val uri = createTrackBitmap(context, track, imperial) ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share track"))
}

/**
 * Export a track as GPX and share it, targeting Strava if installed.
 * Falls back to the generic share chooser when Strava isn't present.
 */
fun shareToStrava(context: Context, track: Track, imperial: Boolean) {
    try {
        val gpxXml = exportGpx(emptyList(), listOf(track))
        val dir = File(context.cacheDir, "shared")
        dir.mkdirs()
        val file = File(dir, "track_${track.id}.gpx")
        file.writeText(gpxXml)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Try to target Strava directly; fall back to chooser if not installed
        val stravaPackage = "com.strava"
        val pm = context.packageManager
        val stravaInstalled = try {
            pm.getPackageInfo(stravaPackage, 0); true
        } catch (_: Exception) { false }
        if (stravaInstalled) {
            intent.setPackage(stravaPackage)
            context.startActivity(intent)
        } else {
            context.startActivity(Intent.createChooser(intent, "Share GPX"))
        }
    } catch (_: Exception) { }
}

private fun createTrackBitmap(context: Context, track: Track, imperial: Boolean): Uri? {
    return try {
        val stats = computeTrackStatsCached(track)
        val tos = computeTimeOnSnow(track.points)

        val d = context.resources.displayMetrics.density
        val w = (380 * d).toInt()
        val h = (560 * d).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background gradient (blue → darker blue)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                android.graphics.Color.parseColor("#0A84FF"),
                android.graphics.Color.parseColor("#003D80"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val white = android.graphics.Color.WHITE
        val whiteSoft = android.graphics.Color.argb(180, 255, 255, 255)

        // Brand
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 18 * d
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("PISTE", 24 * d, 44 * d, brandPaint)

        // Track name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 26 * d
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val nameStr = if (track.name.length > 22) track.name.take(21) + "\u2026" else track.name
        canvas.drawText(nameStr, 24 * d, 88 * d, namePaint)

        // Date
        val dateFmt = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteSoft
            textSize = 14 * d
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(dateFmt.format(java.util.Date(track.startTime)), 24 * d, 110 * d, datePaint)

        // Big stats grid (2x3)
        val gridTop = 150f * d
        val cellW = (w - 48 * d) / 2f
        val cellH = 90f * d
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteSoft
            textSize = 11 * d
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0.05f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 32 * d
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteSoft
            textSize = 14 * d
        }

        fun drawCell(col: Int, row: Int, label: String, value: String, unit: String = "") {
            val x = 24 * d + col * cellW
            val y = gridTop + row * cellH
            canvas.drawText(label.uppercase(), x, y, labelPaint)
            canvas.drawText(value, x, y + 38 * d, valuePaint)
            if (unit.isNotEmpty()) {
                val vw = valuePaint.measureText(value)
                canvas.drawText(unit, x + vw + 4 * d, y + 38 * d, unitPaint)
            }
        }

        val distance = if (imperial) {
            val mi = stats.distanceMeters / 1609.344
            "%.1f".format(mi) to "mi"
        } else {
            if (stats.distanceMeters >= 1000) "%.1f".format(stats.distanceMeters / 1000) to "km"
            else stats.distanceMeters.toInt().toString() to "m"
        }
        val vertical = stats.verticalDescended ?: 0.0
        val verticalStr = if (imperial) (vertical * 3.28084).toInt().toString() to "ft"
                          else vertical.toInt().toString() to "m"
        val maxSpeedStr = if (imperial) "%.0f".format(stats.maxSpeedKmh * 0.621371) to "mph"
                          else "%.0f".format(stats.maxSpeedKmh) to "km/h"

        drawCell(0, 0, "Distance", distance.first, distance.second)
        drawCell(1, 0, "Duration", formatDuration(stats.durationMs), "")
        drawCell(0, 1, "Vertical", verticalStr.first, verticalStr.second)
        drawCell(1, 1, "Max Speed", maxSpeedStr.first, maxSpeedStr.second)
        drawCell(0, 2, "Runs", stats.runCount.toString(), "")
        drawCell(1, 2, "On Snow", "${tos.snowPercent}", "%")

        // Bottom watermark
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteSoft
            textSize = 12 * d
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText("Tracked with Piste", w / 2f, h - 28 * d, watermarkPaint)

        // Save and return URI
        val dir = File(context.cacheDir, "shared")
        dir.mkdirs()
        val file = File(dir, "track_${track.id}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}
