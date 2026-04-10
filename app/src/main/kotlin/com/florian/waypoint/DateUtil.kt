package com.florian.waypoint

import java.util.Calendar

/**
 * Return the timestamp for midnight of the day containing [ms] (in local timezone).
 * Used for "today" filters and day-grouping across the app.
 */
fun startOfDay(ms: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ms
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
