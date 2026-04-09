package com.florian.waypoint

import kotlin.math.*

enum class CoordFormat(val label: String) {
    DECIMAL("Decimal"),
    DMS("DMS"),
    UTM("UTM"),
}

fun formatCoordinate(lat: Double, lon: Double, format: CoordFormat): String = when (format) {
    CoordFormat.DECIMAL -> "%.4f,  %.4f".format(lat, lon)
    CoordFormat.DMS -> "${toDms(lat, isLat = true)},  ${toDms(lon, isLat = false)}"
    CoordFormat.UTM -> toUtm(lat, lon)
}

private fun toDms(value: Double, isLat: Boolean): String {
    val dir = if (isLat) { if (value >= 0) "N" else "S" } else { if (value >= 0) "E" else "W" }
    val abs = abs(value)
    val d = abs.toInt()
    val mFull = (abs - d) * 60
    val m = mFull.toInt()
    val s = (mFull - m) * 60
    return "%d\u00B0%02d'%04.1f\"%s".format(d, m, s, dir)
}

private fun toUtm(lat: Double, lon: Double): String {
    if (lat < -80 || lat > 84) return "%.4f, %.4f".format(lat, lon) // outside UTM range
    val zone = ((lon + 180) / 6).toInt() + 1
    val letter = utmLetter(lat)
    val lonOrigin = (zone - 1) * 6.0 - 180 + 3
    val a = 6378137.0; val f = 1 / 298.257223563
    val e2 = 2 * f - f * f; val ep2 = e2 / (1 - e2)
    val k0 = 0.9996
    val latR = Math.toRadians(lat); val lonR = Math.toRadians(lon - lonOrigin)
    val n = a / sqrt(1 - e2 * sin(latR).pow(2))
    val t = tan(latR).pow(2); val c = ep2 * cos(latR).pow(2)
    val aa = cos(latR) * lonR
    val m0 = a * ((1 - e2 / 4 - 3 * e2.pow(2) / 64) * latR -
            (3 * e2 / 8 + 3 * e2.pow(2) / 32) * sin(2 * latR) +
            (15 * e2.pow(2) / 256) * sin(4 * latR))
    val easting = k0 * n * (aa + (1 - t + c) * aa.pow(3) / 6 + (5 - 18 * t + t.pow(2)) * aa.pow(5) / 120) + 500000
    var northing = k0 * (m0 + n * tan(latR) * (aa.pow(2) / 2 + (5 - t + 9 * c + 4 * c.pow(2)) * aa.pow(4) / 24))
    if (lat < 0) northing += 10000000
    return "%d%c %d %d".format(zone, letter, easting.toInt(), northing.toInt())
}

private fun utmLetter(lat: Double): Char = when {
    lat >= 72 -> 'X'; lat >= 64 -> 'W'; lat >= 56 -> 'V'; lat >= 48 -> 'U'
    lat >= 40 -> 'T'; lat >= 32 -> 'S'; lat >= 24 -> 'R'; lat >= 16 -> 'Q'
    lat >= 8 -> 'P'; lat >= 0 -> 'N'; lat >= -8 -> 'M'; lat >= -16 -> 'L'
    lat >= -24 -> 'K'; lat >= -32 -> 'J'; lat >= -40 -> 'H'; lat >= -48 -> 'G'
    lat >= -56 -> 'F'; lat >= -64 -> 'E'; lat >= -72 -> 'D'; else -> 'C'
}
