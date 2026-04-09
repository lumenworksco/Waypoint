package com.florian.waypoint

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

enum class MapStyle(val label: String) {
    STANDARD("Standard"),
    DARK("Dark"),
    SATELLITE("Satellite"),
}

val CartoDark = XYTileSource(
    "CartoDB Dark Matter",
    0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
    )
)

val EsriSatellite = object : OnlineTileSourceBase(
    "Esri Satellite",
    0, 18, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}$z/$y/$x"
    }
}

fun MapStyle.tileSource() = when (this) {
    MapStyle.STANDARD -> TileSourceFactory.MAPNIK
    MapStyle.DARK -> CartoDark
    MapStyle.SATELLITE -> EsriSatellite
}
