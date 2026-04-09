package com.florian.waypoint

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private val isoFormat get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

fun exportGpx(waypoints: List<Waypoint>, tracks: List<Track> = emptyList()): String {
    val sb = StringBuilder()
    sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    sb.appendLine("""<gpx version="1.1" creator="Waypoint Android">""")

    for (wp in waypoints) {
        sb.appendLine("""  <wpt lat="${wp.latitude}" lon="${wp.longitude}">""")
        sb.appendLine("""    <name>${escapeXml(wp.name)}</name>""")
        if (wp.notes.isNotBlank()) sb.appendLine("""    <desc>${escapeXml(wp.notes)}</desc>""")
        sb.appendLine("""    <time>${isoFormat.format(Date(wp.timestamp))}</time>""")
        sb.appendLine("""    <extensions><color>${wp.color}</color></extensions>""")
        sb.appendLine("""  </wpt>""")
    }

    for (track in tracks) {
        sb.appendLine("""  <trk>""")
        sb.appendLine("""    <name>${escapeXml(track.name)}</name>""")
        sb.appendLine("""    <extensions><color>${track.color}</color></extensions>""")
        sb.appendLine("""    <trkseg>""")
        for (pt in track.points) {
            sb.append("""      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""")
            sb.append("""<time>${isoFormat.format(Date(pt.timestamp))}</time>""")
            pt.altitude?.let { sb.append("""<ele>$it</ele>""") }
            sb.appendLine("""</trkpt>""")
        }
        sb.appendLine("""    </trkseg>""")
        sb.appendLine("""  </trk>""")
    }

    sb.appendLine("""</gpx>""")
    return sb.toString()
}

data class GpxData(val waypoints: List<Waypoint>, val tracks: List<Track>)

fun importGpx(xml: String): GpxData {
    val waypoints = mutableListOf<Waypoint>()
    val tracks = mutableListOf<Track>()
    val parser = Xml.newPullParser()
    parser.setInput(StringReader(xml))

    var inWpt = false
    var inTrk = false
    var inTrkSeg = false
    var inTrkPt = false
    var inExtensions = false
    var lat = 0.0; var lon = 0.0
    var name = ""; var desc = ""; var color = "#3C3734"; var time = 0L
    var trkName = ""; var trkColor = "#007AFF"
    var trkPts = mutableListOf<TrackPoint>()
    var ptLat = 0.0; var ptLon = 0.0; var ptTime = 0L; var ptEle: Double? = null
    var currentTag = ""

    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                when (parser.name) {
                    "wpt" -> {
                        inWpt = true
                        lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                        lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        name = ""; desc = ""; color = "#3C3734"; time = System.currentTimeMillis()
                    }
                    "trk" -> { inTrk = true; trkName = ""; trkColor = "#007AFF"; trkPts = mutableListOf() }
                    "trkseg" -> inTrkSeg = true
                    "trkpt" -> {
                        inTrkPt = true
                        ptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                        ptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        ptTime = System.currentTimeMillis(); ptEle = null
                    }
                    "extensions" -> inExtensions = true
                }
            }
            XmlPullParser.TEXT -> {
                val text = parser.text?.trim() ?: ""
                if (text.isEmpty()) { parser.next(); continue }
                when {
                    inWpt && !inExtensions && currentTag == "name" -> name = text
                    inWpt && !inExtensions && currentTag == "desc" -> desc = text
                    inWpt && inExtensions && currentTag == "color" -> color = text
                    inWpt && currentTag == "time" -> runCatching { time = isoFormat.parse(text)?.time ?: time }
                    inTrk && !inTrkSeg && !inExtensions && currentTag == "name" -> trkName = text
                    inTrk && inExtensions && currentTag == "color" -> trkColor = text
                    inTrkPt && currentTag == "time" -> runCatching { ptTime = isoFormat.parse(text)?.time ?: ptTime }
                    inTrkPt && currentTag == "ele" -> ptEle = text.toDoubleOrNull()
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "wpt" -> {
                        inWpt = false
                        waypoints.add(Waypoint(
                            id = UUID.randomUUID().toString(),
                            name = name.ifBlank { "Imported" },
                            latitude = lat, longitude = lon,
                            notes = desc, timestamp = time, color = color
                        ))
                    }
                    "trkpt" -> {
                        inTrkPt = false
                        trkPts.add(TrackPoint(ptLat, ptLon, ptTime, ptEle))
                    }
                    "trkseg" -> inTrkSeg = false
                    "trk" -> {
                        inTrk = false
                        if (trkPts.isNotEmpty()) {
                            tracks.add(Track(
                                name = trkName.ifBlank { "Imported Track" },
                                points = trkPts.toList(),
                                startTime = trkPts.first().timestamp,
                                endTime = trkPts.last().timestamp,
                                color = trkColor
                            ))
                        }
                    }
                    "extensions" -> inExtensions = false
                }
            }
        }
        parser.next()
    }

    return GpxData(waypoints, tracks)
}

private fun escapeXml(s: String) = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
