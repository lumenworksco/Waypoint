package com.florian.waypoint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

data class Cluster(val items: List<Waypoint>, val centerLat: Double, val centerLon: Double)

fun clusterWaypoints(waypoints: List<Waypoint>, mapView: MapView): List<Cluster> {
    val zoom = mapView.zoomLevelDouble
    if (zoom >= 15 || waypoints.size < 3) {
        return waypoints.map { Cluster(listOf(it), it.latitude, it.longitude) }
    }

    val density = mapView.context.resources.displayMetrics.density
    val cellSize = (60 * density).toInt()
    val grid = mutableMapOf<Long, MutableList<Waypoint>>()

    for (wp in waypoints) {
        val pt = mapView.projection.toPixels(GeoPoint(wp.latitude, wp.longitude), null)
        val key = ((pt.x / cellSize).toLong() shl 32) or (pt.y / cellSize).toLong()
        grid.getOrPut(key) { mutableListOf() }.add(wp)
    }

    return grid.values.map { group ->
        Cluster(
            items = group,
            centerLat = group.sumOf { it.latitude } / group.size,
            centerLon = group.sumOf { it.longitude } / group.size
        )
    }
}

fun createClusterDrawable(context: Context, count: Int): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (40 * density).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f

    canvas.drawCircle(cx, cx, cx - 2 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 0, 122, 255)
        style = Paint.Style.FILL
        setShadowLayer(3 * density, 0f, 1f * density, android.graphics.Color.argb(60, 0, 0, 0))
    })
    canvas.drawCircle(cx, cx, cx - 4 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    })

    val text = if (count > 99) "99+" else count.toString()
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13 * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(text, cx, cx + textPaint.textSize / 3, textPaint)

    return BitmapDrawable(context.resources, bmp)
}
