package com.florian.waypoint

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun createBackupZip(context: Context, outputStream: OutputStream) {
    val prefs = context.getSharedPreferences("waypoints", Context.MODE_PRIVATE)
    ZipOutputStream(outputStream).use { zip ->
        zip.putNextEntry(ZipEntry("waypoints.json"))
        zip.write((prefs.getString("waypoints_json", "[]") ?: "[]").toByteArray())
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("tracks.json"))
        zip.write((prefs.getString("tracks_json", "[]") ?: "[]").toByteArray())
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("settings.json"))
        zip.write(JSONObject().apply {
            put("map_style", prefs.getString("map_style", "STANDARD"))
            put("coord_format", prefs.getString("coord_format", "DECIMAL"))
            put("waypoint_order", prefs.getString("waypoint_order", "[]"))
        }.toString().toByteArray())
        zip.closeEntry()

        val photosDir = File(context.filesDir, "photos")
        if (photosDir.exists()) {
            photosDir.listFiles()?.forEach { file ->
                zip.putNextEntry(ZipEntry("photos/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}

fun restoreBackupZip(context: Context, inputStream: InputStream): Boolean {
    val prefs = context.getSharedPreferences("waypoints", Context.MODE_PRIVATE)
    return try {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "waypoints.json" ->
                        prefs.edit().putString("waypoints_json", zip.bufferedReader().readText()).apply()
                    entry.name == "tracks.json" ->
                        prefs.edit().putString("tracks_json", zip.bufferedReader().readText()).apply()
                    entry.name == "settings.json" -> {
                        val json = JSONObject(zip.bufferedReader().readText())
                        prefs.edit().apply {
                            if (json.has("map_style")) putString("map_style", json.getString("map_style"))
                            if (json.has("coord_format")) putString("coord_format", json.getString("coord_format"))
                            if (json.has("waypoint_order")) putString("waypoint_order", json.getString("waypoint_order"))
                        }.apply()
                    }
                    entry.name.startsWith("photos/") -> {
                        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
                        File(photosDir, entry.name.removePrefix("photos/")).outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        true
    } catch (_: Exception) { false }
}
