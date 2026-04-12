package com.florian.piste

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

/**
 * Full-screen "Memories" slideshow — like Apple Photos Year In Review.
 *
 * Shows today's waypoint photos with a slow Ken Burns zoom, a dark gradient
 * at the bottom carrying stat chips, and navigation dots. Tapping advances
 * to the next photo; tapping on the last one dismisses. Auto-advances every
 * 5 seconds if left untouched.
 *
 * Offered as a "View Memories" button in [SessionRecapSheet] when today has
 * at least one photo-bearing waypoint.
 */
@Composable
fun MemoriesOverlay(
    photos: List<Pair<String, String>>,
    stats: TrackStatistics,
    imperial: Boolean,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) { onDismiss(); return }

    var currentIndex by remember { mutableIntStateOf(0) }
    val photo = photos[currentIndex]

    // Ken Burns zoom: 1.0 → 1.12 for each photo, restarting on advance
    val zoomAnim = remember { Animatable(1.0f) }
    LaunchedEffect(currentIndex) {
        zoomAnim.snapTo(1.0f)
        zoomAnim.animateTo(1.12f, animationSpec = tween(5000, easing = LinearEasing))
    }

    // Auto-advance every 5 seconds
    LaunchedEffect(currentIndex) {
        kotlinx.coroutines.delay(5000)
        if (currentIndex < photos.size - 1) currentIndex++ else onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .iosClickable {
                    if (currentIndex < photos.size - 1) currentIndex++ else onDismiss()
                }
        ) {
            // Photo with Ken Burns zoom (downsampled to avoid OOM)
            val bmp = remember(photo.first) { decodeSampledBitmap(photo.first, 1080, 1920) }
            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = photo.second,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomAnim.value
                            scaleY = zoomAnim.value
                        },
                    contentScale = ContentScale.Crop
                )
            }

            // Dark gradient overlay at bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.35f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.7f)
                        )
                    )
            )

            // Stats and navigation overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    photo.second,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W700,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MemoryStat("Runs", stats.runCount.toString())
                    MemoryStat("Vertical", formatVertical(stats.verticalDescended ?: 0.0, imperial))
                    MemoryStat("Max Speed", formatTrackSpeed(stats.maxSpeedKmh))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MemoryStat("Distance", formatDistance(stats.distanceMeters, imperial).replace(" away", ""))
                    MemoryStat("Time", formatDuration(stats.durationMs))
                }

                // Progress dots
                if (photos.size > 1) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        photos.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (i == currentIndex) Color.White
                                        else Color.White.copy(alpha = 0.35f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.W700, fontSize = 18.sp, color = Color.White)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = Color.White.copy(alpha = 0.7f))
    }
}

/**
 * Decode a bitmap from [path] downsampled so the larger dimension fits within
 * [reqWidth] x [reqHeight]. Uses two-pass decoding: first reads bounds with
 * [BitmapFactory.Options.inJustDecodeBounds], then decodes with an appropriate
 * [BitmapFactory.Options.inSampleSize] to avoid loading the full-resolution
 * image into memory.
 */
private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    // First pass: read dimensions only
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(path, options)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/** Returns photo paths + waypoint names for today's waypoints that have photos. */
fun todaysPhotoPaths(waypoints: List<Waypoint>): List<Pair<String, String>> {
    val dayStart = startOfDay()
    return waypoints
        .filter { it.timestamp >= dayStart && it.photoPath != null && File(it.photoPath!!).exists() }
        .map { it.photoPath!! to it.name }
}
