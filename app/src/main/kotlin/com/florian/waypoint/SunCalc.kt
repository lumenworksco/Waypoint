package com.florian.waypoint

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Compute sunrise and sunset times for a given location and date.
 *
 * Pure local math — no API calls. Uses the standard NOAA algorithm
 * (accurate to within ~1 minute for skiing latitudes and altitudes).
 */
data class SunTimes(val sunrise: Long, val sunset: Long)

fun computeSunTimes(lat: Double, lon: Double, nowMs: Long = System.currentTimeMillis()): SunTimes? {
    return try {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Julian day for 0h UT on this date
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        val jd = jdn - 0.5

        // Julian centuries since J2000
        val n = jd - 2451545.0 + 0.0008

        // Mean solar noon
        val jStar = n - lon / 360.0

        // Solar mean anomaly (degrees)
        val mDeg = (357.5291 + 0.98560028 * jStar) % 360.0
        val mRad = mDeg * PI / 180.0

        // Equation of center
        val c = 1.9148 * sin(mRad) + 0.0200 * sin(2 * mRad) + 0.0003 * sin(3 * mRad)

        // Ecliptic longitude
        val lambdaDeg = (mDeg + c + 180.0 + 102.9372) % 360.0
        val lambdaRad = lambdaDeg * PI / 180.0

        // Solar transit
        val jTransit = 2451545.0 + jStar + 0.0053 * sin(mRad) - 0.0069 * sin(2 * lambdaRad)

        // Declination of the sun
        val sinDec = sin(lambdaRad) * sin(23.4397 * PI / 180.0)
        val dec = asin(sinDec)

        // Hour angle
        val latRad = lat * PI / 180.0
        val cosH = (sin(-0.83 * PI / 180.0) - sin(latRad) * sin(dec)) / (cos(latRad) * cos(dec))
        if (cosH < -1.0 || cosH > 1.0) return null // polar day/night

        val h = acos(cosH) * 180.0 / PI

        val jSet = jTransit + h / 360.0
        val jRise = jTransit - h / 360.0

        SunTimes(
            sunrise = julianToMillis(jRise),
            sunset = julianToMillis(jSet)
        )
    } catch (_: Exception) {
        null
    }
}

private fun julianToMillis(jd: Double): Long {
    return ((jd - 2440587.5) * 86400000.0).toLong()
}

/**
 * Returns minutes until sunset. Negative if sunset has already passed today.
 * Returns null if the sun times couldn't be computed (polar regions).
 */
fun minutesUntilSunset(lat: Double, lon: Double, nowMs: Long = System.currentTimeMillis()): Int? {
    val times = computeSunTimes(lat, lon, nowMs) ?: return null
    return ((times.sunset - nowMs) / 60_000L).toInt()
}

/** Format "42 min" or "1h 12m" for a minute count. */
fun formatMinutesRemaining(mins: Int): String {
    val m = mins.coerceAtLeast(0)
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "$m min"
}
