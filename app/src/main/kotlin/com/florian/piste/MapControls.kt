package com.florian.piste

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

// ── iOS-style map button ────────────────────────────────────────
@Composable
internal fun MapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().iosClickable(onClick), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Zoom pill (Apple Maps style) ────────────────────────────────
@Composable
internal fun ZoomPill(onZoomIn: () -> Unit, onZoomOut: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column(modifier = Modifier.width(48.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).iosClickable(onZoomIn), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, "Zoom in", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).iosClickable(onZoomOut), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Remove, "Zoom out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun RecenterButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().iosClickable { if (enabled) onClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MyLocation, "Center on my location", tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun RecordButton(isRecording: Boolean, isPaused: Boolean = false, onClick: () -> Unit) {
    val bg = when { isRecording && isPaused -> RecordingTrackColor.copy(alpha = 0.45f); isRecording -> RecordingTrackColor; else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.88f) }

    // Pulsing scale animation while actively recording (not paused)
    val pulseScale = if (isRecording && !isPaused) {
        val infiniteTransition = rememberInfiniteTransition(label = "recordPulse")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        ).value
    } else 1f

    Surface(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
        shape = CircleShape,
        color = bg,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().iosScaleClickable(onClick), contentAlignment = Alignment.Center) {
            if (isRecording) Icon(Icons.Filled.Stop, "Stop recording", tint = Color.White, modifier = Modifier.size(22.dp))
            else Icon(Icons.Filled.FiberManualRecord, "Start recording", tint = RecordingTrackColor, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Compose compass (Apple Maps style) ──────────────────────────
@Composable
internal fun ComposeCompass(rotation: Float, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(48.dp).semantics { contentDescription = "Reset map orientation to north" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().iosClickable(onClick), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(20.dp).rotate(-rotation)) {
                val cx = size.width / 2; val cy = size.height / 2
                // Red north half
                val northPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, 0f); lineTo(cx + 5.dp.toPx(), cy); lineTo(cx - 5.dp.toPx(), cy); close()
                }
                drawPath(northPath, Color(0xFFFF3B30))
                // White south half
                val southPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, size.height); lineTo(cx + 5.dp.toPx(), cy); lineTo(cx - 5.dp.toPx(), cy); close()
                }
                drawPath(southPath, Color(0xFFE5E5EA))
            }
        }
    }
}

// ── Compose scale bar (minimal iOS style) ───────────────────────
@Composable
internal fun ComposeScaleBar(metersPerPx: Float, imperial: Boolean = false, modifier: Modifier = Modifier) {
    if (metersPerPx <= 0f) return
    val targetPx = 80f
    val rawMeters = metersPerPx * targetPx

    val label: String
    val barWidth: Float
    if (imperial) {
        val rawFeet = rawMeters * 3.28084f
        val niceFeet = niceRoundImperial(rawFeet)
        barWidth = (niceFeet / 3.28084f / metersPerPx).coerceIn(30f, 150f)
        label = if (niceFeet >= 5280) "${"%.1f".format(niceFeet / 5280f)} mi" else "${niceFeet.toInt()} ft"
    } else {
        val niceMeters = niceRound(rawMeters)
        barWidth = (niceMeters / metersPerPx).coerceIn(30f, 150f)
        label = if (niceMeters >= 1000) "${"%.0f".format(niceMeters / 1000)} km" else "${"%.0f".format(niceMeters)} m"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(modifier = Modifier.width(barWidth.dp).height(6.dp)) {
                val h = size.height; val w = size.width; val stroke = 1.5.dp.toPx()
                drawLine(Color.White, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = stroke)
                drawLine(Color.White, Offset(0f, 0f), Offset(0f, h), strokeWidth = stroke)
                drawLine(Color.White, Offset(w, 0f), Offset(w, h), strokeWidth = stroke)
            }
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.W600, color = Color.White)
        }
    }
}

internal fun niceRoundImperial(feet: Float): Float = when {
    feet >= 26400 -> ((feet / 5280).toInt() * 5280).toFloat()
    feet >= 5280 -> ((feet / 2640).toInt() * 2640).toFloat()
    feet >= 1000 -> ((feet / 500).toInt() * 500).toFloat()
    feet >= 200 -> ((feet / 100).toInt() * 100).toFloat()
    feet >= 50 -> ((feet / 50).toInt() * 50).toFloat()
    else -> ((feet / 10).toInt() * 10).toFloat().coerceAtLeast(10f)
}

internal fun niceRound(meters: Float): Float = when {
    meters >= 5000 -> (meters / 1000).toInt().toFloat() * 1000
    meters >= 1000 -> ((meters / 500).toInt() * 500).toFloat()
    meters >= 200 -> ((meters / 100).toInt() * 100).toFloat()
    meters >= 50 -> ((meters / 50).toInt() * 50).toFloat()
    meters >= 10 -> ((meters / 10).toInt() * 10).toFloat()
    else -> ((meters / 5).toInt() * 5).toFloat().coerceAtLeast(5f)
}

/** Add a speed-colored polyline (each segment colored by its instantaneous speed). */
internal fun addSpeedHeatmap(mapView: MapView, points: List<TrackPoint>, density: Float) {
    if (points.size < 2) return
    // Batch by segments of N points each so we don't create thousands of polylines
    val batchSize = 8
    var i = 0
    while (i < points.size - 1) {
        val end = minOf(i + batchSize, points.size - 1)
        val segment = points.subList(i, end + 1)
        // Average speed for this batch
        var dist = 0.0
        var time = 0L
        for (j in 1 until segment.size) {
            val a = segment[j - 1]; val b = segment[j]
            dist += distanceMeters(GeoPoint(a.latitude, a.longitude), GeoPoint(b.latitude, b.longitude))
            time += b.timestamp - a.timestamp
        }
        val kmh = if (time > 0) (dist / 1000.0) / (time / 3_600_000.0) else 0.0
        val color = speedToColor(kmh)
        mapView.overlays.add(Polyline(mapView).apply {
            setPoints(segment.map { GeoPoint(it.latitude, it.longitude) })
            outlinePaint.color = color
            outlinePaint.strokeWidth = 5f * density
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        })
        i = end
    }
}
