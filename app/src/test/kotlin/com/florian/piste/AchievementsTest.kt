package com.florian.piste

import org.junit.Test
import org.junit.Assert.*

class AchievementsTest {

    @Test
    fun `empty tracks returns zero bests`() {
        val bests = computePersonalBests(emptyList())
        assertEquals(0.0, bests.maxSpeedKmh, 0.01)
        assertEquals(0.0, bests.maxDayVertical, 0.01)
        assertEquals(0, bests.maxDayRuns)
        assertEquals(0.0, bests.maxDayDistance, 0.01)
    }

    @Test
    fun `personal bests accumulate across tracks`() {
        val points1 = listOf(
            TrackPoint(46.0, 7.0, 1000000, 2100.0),
            TrackPoint(46.001, 7.0, 1003000, 2050.0)
        )
        val points2 = listOf(
            TrackPoint(46.0, 7.0, 2000000, 2100.0),
            TrackPoint(46.001, 7.0, 2003000, 2000.0)
        )
        // Both tracks on the same day
        val tracks = listOf(
            Track("t1", "Run 1", points1, 1000000, 1003000),
            Track("t2", "Run 2", points2, 2000000, 2003000)
        )
        val bests = computePersonalBests(tracks)
        assertTrue(bests.maxDayDistance > 0)
    }
}
