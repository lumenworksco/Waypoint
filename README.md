# Piste

A native Android ski companion app built with Kotlin and Jetpack Compose. Track your runs, see your vertical, mark your favorite spots, and navigate any ski resort on OpenStreetMap.

## Features

### Ski-Focused Map
- **Piste Map Layer** — OpenSnowMap tiles with ski runs, difficulty colors, lifts, and ski area boundaries
- **Terrain Layer** — Topographic contours for backcountry and route planning
- **Satellite & Standard** — Esri satellite imagery and classic OpenStreetMap
- **Offline Tiles** — Download map tiles for entire ski areas, use without signal
- **Compass** — Rotates with the map, tap to reset north
- **Scale Bar** — Clean minimal overlay, adapts to zoom level
- **Dark Mode & Glare Mode** — System dark theme, plus high-contrast mode for bright snow

### Run Tracking
- **Live GPS** — Your position on the map with real-time updates
- **Track Recording** — Start, pause, resume, stop your session
- **Foreground Service** — Keeps recording even when your phone is locked
- **Auto Run Detection** — Splits your track into individual descents automatically
- **Lift Auto-Pause** — Ignores lift rides, so your stats stay accurate
- **Daily Stats** — Today's total runs, vertical, max speed, distance, and time
- **Track Stats** — Per-session distance, duration, avg and max speed, elevation gain/loss
- **Vertical Descended** — The stat every skier cares about
- **Elevation Profile** — Visual chart of your altitude over each track
- **Current Speed** — Shown live in the coordinate header when moving

### Waypoints
- **Long-press to add** a waypoint anywhere on the map
- **Ski-specific icons** — lift, gondola, warming hut, restaurant, first aid, powder, cliff, trees, warning, favorite, flag, pin
- **Custom colors** — 6 preset colors for organizing your spots
- **Photos** — attach a gallery photo to any waypoint
- **Notes** — add context to your markers
- **Search** — find saved spots in the list sheet
- **Distance display** — see how far you are from any waypoint
- **Share & Navigate** — share coordinates or open directions
- **Proximity alerts** — vibrate when near a saved waypoint (configurable)

### Weather
- **Live Weather** — current temperature and conditions in the header (via Open-Meteo, no API key)
- **Auto-refresh** — updates every 15 minutes based on your location

### Data
- **GPX Import/Export** — waypoints and tracks in the standard format
- **Full Backup & Restore** — zip of all data (waypoints, tracks, photos, settings)
- **Local Storage** — everything saved on device, no cloud, no accounts

### Settings
- **Distance Units** — Metric or Imperial
- **Default Zoom** — your preferred starting zoom level
- **Proximity Radius** — 0–500m alert distance
- **Tile Cache** — view size and clear offline map cache
- **Glare Mode** — high-contrast UI for sunny snow days

## Architecture

```
app/src/main/kotlin/com/florian/waypoint/
  MainActivity.kt        Entry point, edge-to-edge, theme + glare mode state
  MapScreen.kt           Main screen: map + all UI overlays
  Waypoint.kt            Data classes: Waypoint, Track, TrackPoint
  WaypointStore.kt       SharedPreferences + Gson persistence
  WaypointCard.kt        Detail card, edit sheet, delete sheet
  WaypointListSheet.kt   Bottom sheet with search and track list
  WaypointIcons.kt       12 ski-themed preset waypoint icons
  Theme.kt               Light, dark, and glare color schemes
  MapTiles.kt            5 map styles: Standard, Piste, Terrain, Satellite, Dark
  MapOverlays.kt         Pin drawables, distance, safe color parse, vibration
  MarkerCluster.kt       Grid-based marker clustering
  TrackRecorder.kt       Recording state with pause/resume
  TrackingService.kt     Foreground service for background recording
  TrackStats.kt          Track statistics + run detection + lift filtering
  TrackDetailSheet.kt    Track stats + elevation profile chart
  DailyStatsSheet.kt     Today's aggregate ski stats
  SettingsSheet.kt       Settings UI
  WeatherHelper.kt       Open-Meteo weather fetch
  GpxHelper.kt           GPX import/export with XmlPullParser
  BackupHelper.kt        Zip backup/restore of all app data
  CoordinateFormat.kt    Decimal and DMS coordinate formatting
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Map | osmdroid + OpenStreetMap + OpenSnowMap + OpenTopoMap + Esri |
| GPS | Google Play Services Location |
| Weather | Open-Meteo (free, no key) |
| Persistence | SharedPreferences + Gson |

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

Piste requests location permission solely to display your position on the map, calculate distances, and record tracks. Location data is processed on-device only and is never transmitted to any server (except Open-Meteo for weather, which receives only your approximate location). Your waypoints, tracks, and photos are stored locally on your device.

Map tiles are loaded from public OSM-based tile providers: OpenStreetMap, OpenSnowMap, OpenTopoMap, CartoDB (dark), and Esri (satellite).

The full privacy policy is available at [florianbraun05.github.io/Waypoint](https://florianbraun05.github.io/Waypoint/).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
