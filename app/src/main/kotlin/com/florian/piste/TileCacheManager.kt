package com.florian.piste

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

object TileCacheManager {
    fun cacheSize(context: Context): String {
        val cacheDir = Configuration.getInstance().osmdroidTileCache ?: File(context.cacheDir, "osmdroid/tiles")
        if (!cacheDir.exists()) return "No cached tiles"
        val bytes = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }

    fun clearCache(context: Context) {
        val cacheDir = Configuration.getInstance().osmdroidTileCache ?: File(context.cacheDir, "osmdroid/tiles")
        if (cacheDir.exists()) cacheDir.deleteRecursively()
        val sqlDb = File(context.filesDir?.parentFile, "databases")
        sqlDb.listFiles()?.filter { it.name.startsWith("tile") }?.forEach { it.delete() }
    }
}
