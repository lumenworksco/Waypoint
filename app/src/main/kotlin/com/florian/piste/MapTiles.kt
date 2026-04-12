package com.florian.piste

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

enum class MapStyle(val label: String) {
    STANDARD("Standard"),
    PISTE("Piste"),
    TERRAIN("Terrain"),
    SATELLITE("Satellite"),
    DARK("Dark"),
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

/** OpenSnowMap — OSM-based piste map with ski runs, lifts, and difficulty colors. */
val OpenSnowMap = XYTileSource(
    "OpenSnowMap",
    0, 18, 256, ".png",
    arrayOf("https://tiles.opensnowmap.org/pistes/")
)

/** OpenTopoMap — topographic map with contour lines, useful for backcountry. */
val OpenTopoMap = XYTileSource(
    "OpenTopoMap",
    0, 17, 256, ".png",
    arrayOf(
        "https://a.tile.opentopomap.org/",
        "https://b.tile.opentopomap.org/",
        "https://c.tile.opentopomap.org/",
    )
)

fun MapStyle.tileSource() = when (this) {
    MapStyle.STANDARD -> TileSourceFactory.MAPNIK
    MapStyle.PISTE -> OpenSnowMap
    MapStyle.TERRAIN -> OpenTopoMap
    MapStyle.SATELLITE -> EsriSatellite
    MapStyle.DARK -> CartoDark
}
