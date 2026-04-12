package com.florian.piste

import org.junit.Test
import org.junit.Assert.*

class SunCalcTest {

    @Test
    fun `computeSunTimes returns non-null for normal latitudes`() {
        // Paris, France - should always have sunrise/sunset
        val result = computeSunTimes(48.8566, 2.3522)
        assertNotNull(result)
        assertTrue(result!!.sunrise > 0)
        assertTrue(result.sunset > result.sunrise) // sunset is after sunrise
    }

    @Test
    fun `sunset is after sunrise`() {
        // Chamonix
        val result = computeSunTimes(45.9237, 6.8694)
        assertNotNull(result)
        assertTrue(result!!.sunset > result.sunrise)
        // Should be within reasonable bounds (4-20 hours of daylight)
        val daylightHours = (result.sunset - result.sunrise) / 3_600_000.0
        assertTrue("Daylight hours: $daylightHours", daylightHours in 4.0..20.0)
    }

    @Test
    fun `minutesUntilSunset returns reasonable value`() {
        // Should return a value (positive or negative)
        val mins = minutesUntilSunset(46.0, 7.0)
        // Should be within -1440..1440 (within a day)
        assertNotNull(mins)
        assertTrue("Minutes: $mins", mins!! in -1440..1440)
    }
}
