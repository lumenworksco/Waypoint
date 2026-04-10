package com.florian.waypoint

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(val temperatureC: Double, val windSpeedKmh: Double, val weatherCode: Int)

/**
 * Fetch current weather from Open-Meteo (free, no API key needed).
 * Call from a background thread — performs a blocking network request.
 */
fun fetchWeather(lat: Double, lon: Double): WeatherData? {
    return try {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val json = JSONObject(response)
        val cw = json.getJSONObject("current_weather")
        WeatherData(
            temperatureC = cw.getDouble("temperature"),
            windSpeedKmh = cw.getDouble("windspeed"),
            weatherCode = cw.getInt("weathercode")
        )
    } catch (_: Exception) {
        null
    }
}

fun formatTemperature(celsius: Double, imperial: Boolean): String =
    if (imperial) "${(celsius * 9 / 5 + 32).toInt()}\u00B0F" else "${celsius.toInt()}\u00B0C"

/** Maps Open-Meteo WMO weather codes to a simple emoji/symbol. */
fun weatherEmoji(code: Int): String = when (code) {
    0 -> "\u2600\uFE0F" // clear
    1, 2 -> "\u26C5" // mostly clear / partly cloudy
    3 -> "\u2601\uFE0F" // overcast
    45, 48 -> "\uD83C\uDF2B\uFE0F" // fog
    in 51..57 -> "\uD83C\uDF26\uFE0F" // drizzle
    in 61..67 -> "\uD83C\uDF27\uFE0F" // rain
    in 71..77 -> "\u2744\uFE0F" // snow
    in 80..82 -> "\uD83C\uDF27\uFE0F" // showers
    in 85..86 -> "\uD83C\uDF28\uFE0F" // snow showers
    in 95..99 -> "\u26C8\uFE0F" // thunderstorm
    else -> ""
}
