package com.florian.waypoint

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class MeasureOverlay(
    private val onResult: (String?) -> Unit
) : Overlay() {

    private var point1: GeoPoint? = null
    private var point2: GeoPoint? = null
    var isActive = false
        private set

    fun activate() { isActive = true; reset() }
    fun deactivate() { isActive = false; reset(); onResult(null) }

    fun reset() {
        point1 = null; point2 = null; onResult(null)
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        if (!isActive) return false
        val projection = mapView.projection
        val tapped = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

        when {
            point1 == null -> {
                point1 = tapped
                onResult("Tap second point")
            }
            point2 == null -> {
                point2 = tapped
                val p = point1 ?: return false
                val dist = distanceMeters(p, tapped)
                onResult(formatDistance(dist).replace(" away", ""))
            }
            else -> reset()
        }
        mapView.invalidate()
        return true
    }

    override fun draw(canvas: Canvas, projection: org.osmdroid.views.Projection) {
        val p1 = point1 ?: return
        val density = canvas.density.toFloat().coerceAtLeast(1f)

        val pt1 = projection.toPixels(p1, null)
        canvas.drawCircle(pt1.x.toFloat(), pt1.y.toFloat(), 6 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, 255, 45, 85)
            style = Paint.Style.FILL
        })

        val p2 = point2 ?: return
        val pt2 = projection.toPixels(p2, null)
        canvas.drawCircle(pt2.x.toFloat(), pt2.y.toFloat(), 6 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, 255, 45, 85)
            style = Paint.Style.FILL
        })

        canvas.drawLine(pt1.x.toFloat(), pt1.y.toFloat(), pt2.x.toFloat(), pt2.y.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(180, 255, 45, 85)
                strokeWidth = 2.5f * density
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
        )

        val dist = distanceMeters(p1, p2)
        val label = formatDistance(dist).replace(" away", "")
        val midX = (pt1.x + pt2.x) / 2f
        val midY = (pt1.y + pt2.y) / 2f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(230, 28, 28, 30)
            textSize = 12 * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(220, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val tw = textPaint.measureText(label)
        val pad = 6 * density
        canvas.drawRoundRect(
            midX - tw / 2 - pad, midY - textPaint.textSize - pad,
            midX + tw / 2 + pad, midY + pad,
            6 * density, 6 * density, bgPaint
        )
        canvas.drawText(label, midX, midY - 2 * density, textPaint)
    }
}
