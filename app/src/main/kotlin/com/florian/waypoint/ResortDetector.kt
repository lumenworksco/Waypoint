package com.florian.waypoint

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reverse-geocode a coordinate to a human-readable place name using OSM Nominatim.
 * Free, no API key needed, but rate-limited to 1 request per second.
 * Call from a background thread.
 */
fun fetchPlaceName(lat: Double, lon: Double): String? {
    return try {
        val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&zoom=13&addressdetails=1")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("User-Agent", "com.florian.waypoint/1.0")

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val json = JSONObject(response)
        val address = json.optJSONObject("address") ?: return null

        // Prefer leisure/resort, then town/village/city, then county/region
        address.optString("leisure").ifEmpty { null }
            ?: address.optString("tourism").ifEmpty { null }
            ?: address.optString("village").ifEmpty { null }
            ?: address.optString("town").ifEmpty { null }
            ?: address.optString("city").ifEmpty { null }
            ?: address.optString("municipality").ifEmpty { null }
            ?: address.optString("county").ifEmpty { null }
    } catch (_: Exception) {
        null
    }
}
