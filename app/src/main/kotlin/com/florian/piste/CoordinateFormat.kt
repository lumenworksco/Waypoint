package com.florian.piste

import kotlin.math.abs

enum class CoordFormat(val label: String) {
    DECIMAL("Decimal"),
    DMS("DMS"),
}

fun formatCoordinate(lat: Double, lon: Double, format: CoordFormat): String = when (format) {
    CoordFormat.DECIMAL -> "%.4f,  %.4f".format(lat, lon)
    CoordFormat.DMS -> "${toDms(lat, isLat = true)},  ${toDms(lon, isLat = false)}"
}

private fun toDms(value: Double, isLat: Boolean): String {
    val dir = if (isLat) { if (value >= 0) "N" else "S" } else { if (value >= 0) "E" else "W" }
    val a = abs(value)
    val d = a.toInt()
    val mFull = (a - d) * 60
    val m = mFull.toInt()
    val s = (mFull - m) * 60
    return "%d\u00B0%02d'%04.1f\"%s".format(d, m, s, dir)
}
