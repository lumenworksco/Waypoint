package com.florian.waypoint

import kotlin.math.max

/**
 * Avalanche awareness — NOT a forecast.
 *
 * The official way to get avalanche danger ratings in Europe is the EAWS network
 * of national services (avalanche.report, lawinen.report, meteofrance.com, slf.ch,
 * etc.), each of which publishes region-specific bulletins in CAAML format. A
 * proper integration would require shipping ~500KB of region GeoJSON for every
 * EAWS country and doing point-in-polygon matching plus per-country API calls.
 *
 * That's out of scope for this app — and more importantly, faking a danger level
 * from inferred weather data would be irresponsible. Instead we compute a simple
 * "elevated conditions" flag from the Open-Meteo hourly forecast that we already
 * fetch, and link out to the user's local official bulletin for the real number.
 *
 * The flag fires on any of:
 *   - Recent (24h) new snow ≥ 20 cm
 *   - Sustained wind ≥ 40 km/h combined with any new snow (wind-loading)
 *   - Rapid temperature swing > 10°C in 24h (wet-snow instability)
 */
data class AwarenessAssessment(
    val elevated: Boolean,
    val snowCm24h: Double,
    val maxWindKmh: Double,
    val tempSwingC: Double,
    val reasons: List<String>
)

fun assessAvalancheAwareness(forecast: Forecast?): AwarenessAssessment? {
    val f = forecast ?: return null
    // Use up to 24 hours from the hourly forecast
    val hours = f.hourly.take(24)
    if (hours.size < 4) return null

    val snow24 = hours.sumOf { it.snowCm }
    val maxWind = hours.maxOf { it.windKmh }
    val minTemp = hours.minOf { it.tempC }
    val maxTemp = hours.maxOf { it.tempC }
    val swing = max(0.0, maxTemp - minTemp)

    val reasons = mutableListOf<String>()
    if (snow24 >= 20.0) reasons.add("New snow ${snow24.toInt()} cm expected in 24h")
    if (maxWind >= 40.0 && snow24 >= 5.0) reasons.add("Wind-loading: gusts to ${maxWind.toInt()} km/h with fresh snow")
    if (swing >= 10.0) reasons.add("Temperature swing ${swing.toInt()}°C in 24h")

    return AwarenessAssessment(
        elevated = reasons.isNotEmpty(),
        snowCm24h = snow24,
        maxWindKmh = maxWind,
        tempSwingC = swing,
        reasons = reasons
    )
}

/**
 * Return the URL of the local avalanche bulletin for a given place name.
 * Falls back to avalanche.report (covers most of the Alps).
 *
 * We use the Nominatim place name rather than strict country detection because
 * Nominatim's country resolution is already working elsewhere in the app.
 */
fun localBulletinUrl(placeName: String?): String {
    val name = placeName?.lowercase() ?: return DEFAULT_BULLETIN_URL
    return when {
        // France
        "france" in name || "alpes" in name || "savoie" in name || "chamonix" in name || "val" in name -> "https://meteofrance.com/meteo-montagne/bulletin-avalanches"
        // Switzerland
        "switzerland" in name || "schweiz" in name || "suisse" in name || "wallis" in name || "valais" in name || "graubünden" in name -> "https://www.slf.ch/en/avalanche-bulletin-and-snow-situation.html"
        // Austria
        "austria" in name || "österreich" in name || "tirol" in name || "tyrol" in name || "salzburg" in name || "vorarlberg" in name -> "https://avalanche.report/"
        // Italy
        "italy" in name || "italia" in name || "südtirol" in name || "alto adige" in name || "dolomites" in name || "dolomiti" in name -> "https://avalanche.report/"
        // Germany
        "germany" in name || "deutschland" in name || "bayern" in name || "bavaria" in name -> "https://lawinenwarndienst-bayern.de/"
        // USA
        "colorado" in name || "utah" in name || "california" in name || "washington" in name || "wyoming" in name || "united states" in name || "usa" in name -> "https://avalanche.org/"
        // Canada
        "canada" in name || "british columbia" in name || "alberta" in name || "quebec" in name -> "https://www.avalanche.ca/"
        // Norway
        "norway" in name || "norge" in name -> "https://varsom.no/en/avalanche-bulletins/"
        // Scotland
        "scotland" in name || "highlands" in name -> "https://www.sais.gov.uk/"
        else -> DEFAULT_BULLETIN_URL
    }
}

/** Pan-European fallback covering the whole EAWS network. */
private const val DEFAULT_BULLETIN_URL = "https://www.avalanches.org/bulletins/"
