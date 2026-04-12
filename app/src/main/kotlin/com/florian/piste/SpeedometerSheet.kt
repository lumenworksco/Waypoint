package com.florian.piste

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Live dashboard sheet showing the user's current speed as a huge number,
 * plus altitude, vertical descended, and run count for the current recording.
 * Opened by tapping the coordinate header on the map screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedometerSheet(
    currentSpeedKmh: Double,
    altitudeMeters: Double?,
    currentRunVertical: Double?,
    currentRunCount: Int,
    imperial: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { DragHandle() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Live Dashboard", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize().iosClickable(onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Big speed display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val speedValue = if (imperial) currentSpeedKmh * 0.621371 else currentSpeedKmh
                AnimatedNumber(
                    value = speedValue.toFloat(),
                    decimals = 0,
                    fontWeight = FontWeight.W700,
                    fontSize = 120.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (imperial) "mph" else "km/h",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Secondary stats grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (altitudeMeters != null) {
                    BigStat("Altitude", formatVertical(altitudeMeters, imperial))
                }
                if (currentRunVertical != null) {
                    BigStat("Vertical", formatVertical(currentRunVertical, imperial))
                }
                if (currentRunCount > 0) {
                    BigStat("Runs", currentRunCount.toString())
                }
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.width(110.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.W700, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
        }
    }
}
