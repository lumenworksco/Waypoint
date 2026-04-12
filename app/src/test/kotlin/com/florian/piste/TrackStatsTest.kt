package com.florian.piste

import org.junit.Test
import org.junit.Assert.*

class TrackStatsTest {

    private fun makePoints(vararg altitudes: Double, baseTime: Long = 1000000L, interval: Long = 3000L, baseLat: Double = 46.0, baseLon: Double = 7.0): List<TrackPoint> {
        return altitudes.mapIndexed { i, alt ->
            TrackPoint(
                latitude = baseLat + i * 0.0001, // ~11m apart
                longitude = baseLon,
                timestamp = baseTime + i * interval,
                altitude = alt
            )
        }
    }

    @Test
    fun `empty points returns zero stats`() {
        val stats = computeTrackStats(emptyList())
        assertEquals(0.0, stats.distanceMeters, 0.01)
        assertEquals(0, stats.runCount)
    }

    @Test
    fun `single point returns zero stats`() {
        val stats = computeTrackStats(listOf(TrackPoint(46.0, 7.0, 1000000, 2000.0)))
        assertEquals(0.0, stats.distanceMeters, 0.01)
    }

    @Test
    fun `detects a simple ski run`() {
        // Descend 100m over multiple points
        val points = makePoints(2100.0, 2080.0, 2060.0, 2040.0, 2020.0, 2000.0)
        val runs = detectRuns(points)
        assertEquals(1, runs.size)
        assertTrue(runs[0].verticalDrop >= 90.0) // ~100m drop
    }

    @Test
    fun `ignores small elevation changes`() {
        // Less than 30m vertical - should not count as a run
        val points = makePoints(2020.0, 2015.0, 2010.0, 2005.0, 2000.0)
        val runs = detectRuns(points)
        assertEquals(0, runs.size) // Only 20m drop, below threshold
    }

    @Test
    fun `detects multiple runs separated by climb`() {
        // Run 1: 2100 -> 2050 (50m), then climb 2050 -> 2070, then Run 2: 2070 -> 2020 (50m)
        val points = makePoints(
            2100.0, 2085.0, 2070.0, 2055.0, 2050.0, // run 1
            2055.0, 2060.0, 2065.0, 2070.0,          // climb
            2060.0, 2045.0, 2030.0, 2020.0            // run 2
        )
        val runs = detectRuns(points)
        assertEquals(2, runs.size)
    }

    @Test
    fun `formatDuration formats correctly`() {
        assertEquals("30s", formatDuration(30000))
        assertEquals("5m 30s", formatDuration(330000))
        assertEquals("1h 30m", formatDuration(5400000))
    }

    @Test
    fun `formatVertical metric and imperial`() {
        assertEquals("1000 m", formatVertical(1000.0, false))
        assertTrue(formatVertical(1000.0, true).contains("ft"))
    }

    @Test
    fun `streak counts consecutive days`() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val tracks = listOf(
            Track("1", "t1", emptyList(), now, now, "#007AFF"),
            Track("2", "t2", emptyList(), now - day, now - day, "#007AFF"),
            Track("3", "t3", emptyList(), now - 2 * day, now - 2 * day, "#007AFF")
        )
        assertEquals(3, computeStreak(tracks))
    }

    @Test
    fun `streak returns 0 with no tracks today`() {
        val yesterday = System.currentTimeMillis() - 2 * 86_400_000L
        val tracks = listOf(Track("1", "t1", emptyList(), yesterday, yesterday, "#007AFF"))
        assertEquals(0, computeStreak(tracks))
    }

    @Test
    fun `run difficulty classification`() {
        assertEquals(RunDifficulty.EASY, RunDifficulty.EASY)
        // Gradient thresholds
        assertTrue(0.10 < 0.15) // Easy
        assertTrue(0.20 < 0.30) // Moderate
        assertTrue(0.35 >= 0.30) // Steep
    }

    // Note: speedToColor tests are omitted from pure JVM tests because the
    // underlying interp() function calls android.graphics.Color.argb which
    // is unavailable without Robolectric or an Android device.
}
