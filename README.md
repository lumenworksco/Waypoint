# Waypoint

An Android app for dropping and managing geographical waypoints on OpenStreetMap. Built with Kotlin and Jetpack Compose.

## Features

### Map
- **Interactive Map** -- Full-screen OpenStreetMap with pinch-to-zoom, pan, and two-finger rotation
- **Map Styles** -- Switch between Standard, Dark (CartoDB), and Satellite (Esri) tile layers
- **Compass** -- Rotates with the map, tap to reset north
- **Scale Bar** -- Adapts to zoom level, supports metric and imperial
- **Marker Clustering** -- Groups nearby waypoints when zoomed out
- **Offline Tiles** -- Download map tiles for offline use, manage cache in settings
- **Dark Mode** -- Follows system theme with matching UI colors

### Waypoints
- **Drop Waypoints** -- Long-press anywhere on the map to place a new waypoint
- **Edit & Manage** -- Tap a waypoint to view, edit name/notes, or delete it
- **Custom Colors** -- Choose from 6 preset colors per waypoint
- **Custom Icons** -- Pick from 12 preset icons (parking, campsite, restaurant, etc.)
- **Photos** -- Attach a photo from your gallery to any waypoint
- **Distance Display** -- See how far you are from any selected waypoint
- **Share** -- Share waypoint coordinates and a Google Maps link
- **Navigate** -- Open directions in Google Maps or any navigation app
- **QR Code** -- Generate a scannable QR code for any waypoint's location
- **Search & Reorder** -- Search waypoints by name, manually reorder the list

### Tracking
- **Live GPS** -- Your position shown as a blue dot with real-time updates
- **Speed Indicator** -- Current speed shown in the coordinate header when moving
- **Track Recording** -- Record your path with start/pause/resume/stop
- **Foreground Service** -- Recording continues when the app is backgrounded
- **Track Stats** -- Distance, duration, average speed, elevation gain/loss
- **Elevation Profile** -- Visual chart of altitude over distance for recorded tracks
- **Proximity Alerts** -- Vibrate when near a saved waypoint (configurable radius)

### Data
- **GPX Import/Export** -- Import and export waypoints and tracks as GPX files
- **Import from Link** -- Paste a Google Maps URL to create a waypoint
- **Backup & Restore** -- Export all data (waypoints, tracks, settings, photos) as a zip
- **Auto-Center** -- Map centers on your location when GPS first locks on
- **Offline Persistence** -- All data saved locally, survives app restarts
- **Coordinate Formats** -- Tap the header to cycle between Decimal, DMS, and UTM

### Settings
- **Distance Units** -- Metric (km/m) or Imperial (mi/ft)
- **Default Zoom** -- Set your preferred zoom level (10--19)
- **Proximity Radius** -- Configure alert distance (0--500m)
- **Tile Cache** -- View size and clear offline map cache
- **Measure Distance** -- Tap two points on the map to measure straight-line distance

## Architecture

```
app/src/main/kotlin/com/florian/waypoint/
  MainActivity.kt        Entry point, edge-to-edge, Compose theme
  MapScreen.kt           Main screen: map + all UI overlays
  Waypoint.kt            Data classes: Waypoint, Track, TrackPoint
  WaypointStore.kt       SharedPreferences + Gson persistence
  WaypointCard.kt        Detail card, edit sheet, QR sheet, delete sheet
  WaypointListSheet.kt   Bottom sheet with search, reorder, track list
  WaypointIcons.kt       12 preset waypoint icons
  Theme.kt               Light/dark color schemes, iOS-style press modifier
  MapTiles.kt            Map style enum + custom tile sources
  MapOverlays.kt         Pin/dot drawables, distance, vibration helpers
  TrackRecorder.kt       Recording state with pause/resume
  TrackingService.kt     Foreground service for background recording
  TrackStats.kt          Track statistics computation
  TrackDetailSheet.kt    Track stats + elevation profile chart
  SettingsSheet.kt       Settings UI (units, zoom, proximity, cache)
  GpxHelper.kt           GPX import/export with XmlPullParser
  BackupHelper.kt        Zip backup/restore of all app data
  QrCodeHelper.kt        QR code bitmap generation via ZXing
  MarkerCluster.kt       Grid-based marker clustering
  MeasureOverlay.kt      Two-tap distance measurement overlay
  CoordinateFormat.kt    Decimal, DMS, UTM coordinate formatting
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Map | osmdroid + OpenStreetMap |
| GPS | Google Play Services Location |
| Persistence | SharedPreferences + Gson |
| QR Codes | ZXing |

## Getting Started

### Prerequisites

- Android Studio (or the Android SDK)
- JDK 17 or 21

### Build

```bash
./gradlew assembleDebug
```

### Install on a connected device

```bash
./gradlew installDebug
```

## Privacy

Waypoint requests location permission solely to display your position on the map and calculate distances to your waypoints. Location data is processed on-device only and is never transmitted to any server. Waypoint data is stored locally on your device using SharedPreferences.

The app makes network requests only to load OpenStreetMap tile images.

The full privacy policy is available at [florianbraun05.github.io/Waypoint](https://florianbraun05.github.io/Waypoint/).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
