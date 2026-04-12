package com.florian.piste

import org.junit.Test
import org.junit.Ignore
import org.junit.Assert.*

/**
 * GPX export tests (pure JVM) and import roundtrip tests (require Android).
 *
 * Tests that call importGpx() are @Ignored because importGpx uses
 * android.util.Xml which is unavailable in pure JVM tests. Move them
 * to androidTest/ or add Robolectric to run them.
 */
class GpxHelperTest {

    @Ignore("Requires android.util.Xml — run as instrumented test")
    @Test
    fun `export and reimport waypoints roundtrip`() {
        val waypoints = listOf(
            Waypoint(id = "w1", name = "Test Point", latitude = 46.0, longitude = 7.0, notes = "A note", color = "#007AFF"),
            Waypoint(id = "w2", name = "Point 2", latitude = 46.5, longitude = 7.5, notes = "", color = "#34C759")
        )
        val gpx = exportGpx(waypoints, emptyList())
        assertTrue(gpx.contains("<wpt"))
        assertTrue(gpx.contains("Test Point"))

        val imported = importGpx(gpx)
        assertEquals(2, imported.waypoints.size)
        assertEquals("Test Point", imported.waypoints[0].name)
    }

    @Ignore("Requires android.util.Xml — run as instrumented test")
    @Test
    fun `export and reimport tracks roundtrip`() {
        val points = listOf(
            TrackPoint(46.0, 7.0, 1000000, 2000.0),
            TrackPoint(46.001, 7.001, 1003000, 1980.0)
        )
        val tracks = listOf(
            Track(id = "t1", name = "Run 1", points = points, startTime = 1000000, endTime = 1003000, color = "#FF2D55")
        )
        val gpx = exportGpx(emptyList(), tracks)
        assertTrue(gpx.contains("<trk"))
        assertTrue(gpx.contains("Run 1"))

        val imported = importGpx(gpx)
        assertEquals(1, imported.tracks.size)
        assertEquals(2, imported.tracks[0].points.size)
    }

    @Test
    fun `empty export produces valid GPX`() {
        val gpx = exportGpx(emptyList(), emptyList())
        assertTrue(gpx.contains("<?xml"))
        assertTrue(gpx.contains("<gpx"))
        assertTrue(gpx.contains("</gpx>"))
    }

    @Test
    fun `special characters in names are escaped`() {
        val waypoints = listOf(
            Waypoint(id = "w1", name = "Test & <Point>", latitude = 46.0, longitude = 7.0)
        )
        val gpx = exportGpx(waypoints, emptyList())
        // Should not contain unescaped special chars
        assertFalse(gpx.contains("Test & <Point>"))
        assertTrue(gpx.contains("&amp;") || gpx.contains("&#"))
    }
}
