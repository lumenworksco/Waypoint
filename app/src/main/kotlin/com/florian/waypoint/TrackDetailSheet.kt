package com.florian.waypoint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailSheet(
    track: Track,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val stats = remember(track) { computeTrackStats(track.points) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(track.color)), CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Text(track.name, fontWeight = FontWeight.W600, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Distance", formatDistance(stats.distanceMeters).replace(" away", ""))
                StatItem("Duration", formatDuration(stats.durationMs))
                StatItem("Avg Speed", formatSpeed(stats.avgSpeedKmh))
            }

            if (stats.elevationGain != null || stats.elevationLoss != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    stats.elevationGain?.let { StatItem("Elev. Gain", "+${it.toInt()} m") }
                    stats.elevationLoss?.let { StatItem("Elev. Loss", "-${it.toInt()} m") }
                }
            }

            // Elevation profile
            val altitudes = track.points.mapNotNull { it.altitude?.takeIf { a -> a != 0.0 } }
            if (altitudes.size >= 3) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Elevation Profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                ElevationChart(altitudes = altitudes, color = Color(android.graphics.Color.parseColor(track.color)))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Delete button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Track")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.W600, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ElevationChart(altitudes: List<Double>, color: Color) {
    // Smooth with 5-point moving average
    val smoothed = if (altitudes.size >= 5) {
        altitudes.indices.map { i ->
            val start = maxOf(0, i - 2)
            val end = minOf(altitudes.size, i + 3)
            altitudes.subList(start, end).average()
        }
    } else altitudes

    val minAlt = smoothed.min()
    val maxAlt = smoothed.max()
    val range = (maxAlt - minAlt).coerceAtLeast(1.0)
    val lineColor = color
    val fillColor = color.copy(alpha = 0.15f)

    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Box {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(12.dp)) {
                val w = size.width; val h = size.height
                if (smoothed.size < 2) return@Canvas

                val path = Path()
                val fillPath = Path()
                fillPath.moveTo(0f, h)

                smoothed.forEachIndexed { i, alt ->
                    val x = i.toFloat() / (smoothed.size - 1) * w
                    val y = h - ((alt - minAlt) / range * h).toFloat()
                    if (i == 0) { path.moveTo(x, y); fillPath.lineTo(x, y) }
                    else { path.lineTo(x, y); fillPath.lineTo(x, y) }
                }

                fillPath.lineTo(w, h)
                fillPath.close()
                drawPath(fillPath, fillColor)
                drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
            }

            // Axis labels
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).align(Alignment.TopStart)) {
                Text("${maxAlt.toInt()} m", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).align(Alignment.BottomStart)) {
                Text("${minAlt.toInt()} m", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
