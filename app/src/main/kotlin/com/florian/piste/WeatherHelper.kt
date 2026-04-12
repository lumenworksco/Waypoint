package com.florian.piste

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(val temperatureC: Double, val windSpeedKmh: Double, val weatherCode: Int)
data class ForecastHour(val time: Long, val tempC: Double, val code: Int, val windKmh: Double, val snowCm: Double)
data class Forecast(val current: WeatherData, val hourly: List<ForecastHour>)

/**
 * Fetch current weather from Open-Meteo (free, no API key needed).
 * Call from a background thread.
 */
fun fetchWeather(lat: Double, lon: Double): WeatherData? {
    return try {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000; conn.readTimeout = 5000
        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val cw = JSONObject(response).getJSONObject("current_weather")
        WeatherData(
            temperatureC = cw.getDouble("temperature"),
            windSpeedKmh = cw.getDouble("windspeed"),
            weatherCode = cw.getInt("weathercode")
        )
    } catch (_: Exception) { null }
}

/**
 * Fetch 24-hour forecast + current from Open-Meteo.
 */
fun fetchForecast(lat: Double, lon: Double): Forecast? {
    return try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current_weather=true&hourly=temperature_2m,weathercode,windspeed_10m,snowfall&forecast_days=2"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000; conn.readTimeout = 5000
        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val json = JSONObject(response)
        val cw = json.getJSONObject("current_weather")
        val current = WeatherData(
            temperatureC = cw.getDouble("temperature"),
            windSpeedKmh = cw.getDouble("windspeed"),
            weatherCode = cw.getInt("weathercode")
        )

        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weathercode")
        val winds = hourly.getJSONArray("windspeed_10m")
        val snows = hourly.getJSONArray("snowfall")

        val isoFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        isoFmt.timeZone = java.util.TimeZone.getDefault()

        val nowMs = System.currentTimeMillis()
        val hours = mutableListOf<ForecastHour>()
        for (i in 0 until times.length()) {
            val t: Long = try {
                isoFmt.parse(times.getString(i))?.time ?: continue
            } catch (_: Exception) {
                continue
            }
            if (t < nowMs - 3_600_000) continue // skip past hours
            hours.add(ForecastHour(
                time = t,
                tempC = temps.getDouble(i),
                code = codes.getInt(i),
                windKmh = winds.getDouble(i),
                snowCm = snows.getDouble(i) // open-meteo snowfall is already in cm
            ))
            if (hours.size >= 24) break
        }
        Forecast(current, hours)
    } catch (_: Exception) { null }
}

fun formatTemperature(celsius: Double, imperial: Boolean): String =
    if (imperial) "${(celsius * 9 / 5 + 32).toInt()}\u00B0F" else "${celsius.toInt()}\u00B0C"

fun formatWind(kmh: Double, imperial: Boolean): String =
    if (imperial) "${(kmh * 0.621371).toInt()} mph" else "${kmh.toInt()} km/h"

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
