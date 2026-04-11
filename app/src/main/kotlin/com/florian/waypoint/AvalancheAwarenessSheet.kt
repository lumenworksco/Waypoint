package com.florian.waypoint

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shown when the user taps the avalanche-awareness pill. Lists the observed
 * elevated conditions and provides a prominent button to open the official
 * local EAWS bulletin in a browser.
 *
 * Explicitly tells the user this is NOT a forecast — they should always check
 * the official bulletin before going off-piste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvalancheAwarenessSheet(
    assessment: AwarenessAssessment,
    placeName: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bulletinUrl = localBulletinUrl(placeName)

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Avalanche Awareness",
                    fontWeight = FontWeight.W600,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Current weather shows conditions that can increase avalanche risk. " +
                    "This is based on snowfall, wind, and temperature — not an official forecast.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Reasons
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    assessment.reasons.forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u2022", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                reason,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Before leaving groomed runs, always check your local avalanche bulletin. " +
                    "Carry a transceiver, shovel, and probe; ski with a partner; know your terrain.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Open bulletin button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosClickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bulletinUrl))
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Open Official Bulletin",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
