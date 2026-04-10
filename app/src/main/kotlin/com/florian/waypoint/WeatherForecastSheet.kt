package com.florian.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet showing current weather conditions and an hourly forecast.
 * Data comes from Open-Meteo (free, no API key). Displays temperature, wind speed,
 * a high-wind warning when appropriate, a 24-hour scrollable forecast, and the total
 * expected snowfall in the next 24 hours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherForecastSheet(forecast: Forecast?, imperial: Boolean, placeName: String?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Weather", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            placeName?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (forecast == null) {
                Text(
                    "Could not load weather. Check your connection.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                return@Column
            }

            // Current conditions — large
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(weatherEmoji(forecast.current.weatherCode), fontSize = 56.sp)
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        formatTemperature(forecast.current.temperatureC, imperial),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.W300,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${formatWind(forecast.current.windSpeedKmh, imperial)} wind",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Wind warning
            if (forecast.current.windSpeedKmh >= 50.0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)) {
                    Text(
                        "\u26A0 High winds — lifts may close",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Next 24 Hours", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            // Hourly scroll
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                forecast.hourly.forEach { hour ->
                    HourCard(hour = hour, imperial = imperial)
                }
            }

            // Snowfall summary
            val totalSnow = forecast.hourly.sumOf { it.snowCm }
            if (totalSnow > 0.1) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\u2744\uFE0F", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${"%.1f".format(totalSnow)} cm snow expected in the next 24h",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourCard(hour: ForecastHour, imperial: Boolean) {
    val timeFmt = remember { java.text.SimpleDateFormat("h a", java.util.Locale.getDefault()) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.width(64.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(timeFmt.format(java.util.Date(hour.time)), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(4.dp))
            Text(weatherEmoji(hour.code), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatTemperature(hour.tempC, imperial),
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hour.snowCm >= 0.1) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${"%.1f".format(hour.snowCm)}\u2009cm",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W600
                )
            }
        }
    }
}
