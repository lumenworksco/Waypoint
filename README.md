# Piste

A native Android ski companion app built with Kotlin and Jetpack Compose. Track your runs, see your vertical, mark your favorite spots, and navigate any ski resort on OpenStreetMap.

Crafted in the French Alps.

## Features

### Ski-Focused Map
- **5 map styles** — Standard OSM, Piste (OpenSnowMap with ski runs + difficulty colors + lifts), Terrain (OpenTopoMap contours), Satellite (Esri imagery), and Dark (CartoDB)
- **Offline tiles** — download visible areas for use without signal, manage cache in settings
- **Compass** — always-visible north indicator that rotates with the map, tap to reset north
- **Scale bar** — minimal overlay that adapts to zoom level and units
- **Marker clustering** — nearby waypoints group into count bubbles when zoomed out
- **Resort detection** — reverse-geocodes your location to show the current resort/town in the header
- **Dark mode & Glare mode** — follows the system theme, with an optional high-contrast mode for bright snow
- **Full edge-to-edge layout** — the map reaches from status bar to nav bar

### Run Tracking
- **Foreground service** — recording continues even when the phone is locked or in your pocket
- **Start, pause, resume, stop** — full control over each session
- **Auto-follow mode** — map keeps you centered while recording; disengages if you touch the map
- **Auto run detection** — splits your track into individual descents of 30m+ drop
- **Lift auto-pause** — speed on lifts is excluded from your max-speed stat automatically
- **Air time detection** — uses the accelerometer to detect jumps and measures their duration
- **Track stats** — distance, duration, avg and max speed, vertical descended, elevation gain/loss
- **Per-run breakdown** — every detected run shown with difficulty (green/blue/black), drop, duration, and max speed
- **Time on snow vs lifts** — percentage of time actually skiing vs riding lifts
- **Elevation profile chart** — visual altitude graph for each recorded track
- **Speed heatmap** — selected track is drawn as a polyline colored by instantaneous speed
- **Auto resort naming** — new tracks are named "<Resort> — <Date>" when a resort is detected

### Live Dashboard
- **Coordinate header** — shows current lat/lon, altitude, place name, and live speed when moving
- **Coordinate format toggle** — tap to cycle between Decimal and DMS
- **Speedometer sheet** — tap the header for a full-screen live view with huge speed, altitude, vertical, and run count
- **Weather pill** — current temperature, wind, and a high-wind warning when lifts may close
- **Home waypoint** — set any waypoint as "home" and get a compass-back pill showing direction and distance

### Stats & History
- **Daily stats** — runs, vertical, max speed, distance, time, on-snow %, first/last session times, and a pace-of-day chart
- **All-time stats** — lifetime totals plus personal bests (top speed, best day, most runs)
- **Monthly breakdown** — per-month totals sorted newest first
- **Streak counter** — consecutive days recorded
- **Personal bests** — auto-tracked and celebrated with banner notifications when broken
- **Achievement banners** — "🏁 New top speed!", "🏆 Daily vertical record!", etc.
- **Home screen widget** — today's runs and vertical on your launcher, tap to open the app

### Waypoints
- **Long-press to add** — drop a waypoint anywhere on the map
- **12 ski-themed icons** — pin, lift, gondola, hut, food, first aid, powder, cliff, trees, warning, star, flag
- **6 preset colors** — organize your spots by category
- **Photos** — attach a gallery photo to any waypoint
- **Notes** — add context
- **Search** — filter waypoints by name or notes in the list sheet
- **Distance display** — see how far you are from any selected waypoint
- **Share, Navigate, Set Home** — share coordinates, open directions in Google Maps, or mark as home
- **Proximity alerts** — vibrate when within a configurable radius of any saved waypoint
- **Undo delete** — iOS-style toast with Undo after deletion

### Weather
- **Current conditions** — temperature, wind speed, weather code (sun, cloud, rain, snow, etc.)
- **24-hour forecast** — hourly temperature, conditions, and snowfall (tap the weather pill)
- **Snowfall total** — "X cm expected in the next 24h"
- **High-wind warning** — pill turns red when winds reach lift-closure thresholds
- **Powered by Open-Meteo** — free, no API key required

### Data
- **GPX import/export** — waypoints and tracks in the standard format
- **Full backup & restore** — zip of all data (waypoints, tracks, photos, settings)
- **Local storage only** — nothing in the cloud, no accounts

### Settings
- **Distance units** — Metric (km/m) or Imperial (mi/ft)
- **Default zoom** — starting zoom level (10–19)
- **Proximity alert radius** — 0–500m
- **Glare mode** — high-contrast UI for bright snow
- **Keep screen on while recording** — stay awake during a session
- **Offline tile cache** — view size and clear
- **First-launch onboarding** — 5 swipeable cards explaining the core features
- **Easter egg** — tap the "Piste" logo in the About section 7 times

## Architecture

```
app/src/main/kotlin/com/florian/waypoint/
  MainActivity.kt          Entry point, edge-to-edge, theme + glare mode state
  MapScreen.kt             Main screen — map, overlays, sheet management
  Theme.kt                 Light, dark, and glare color schemes + iosClickable modifier
  Waypoint.kt              Data classes: Waypoint, TrackPoint, Track
  WaypointStore.kt         SharedPreferences + Gson persistence
  DateUtil.kt              Shared start-of-day helper

  // Map
  MapTiles.kt              5 map styles and their tile sources
  MapOverlays.kt           Pin drawables, user dot, distance, bearing, vibration
  MarkerCluster.kt         Grid-based marker clustering

  // Tracking
  TrackRecorder.kt         Compose state holder for recording
  TrackingService.kt       Foreground service for background recording
  TrackStats.kt            Stats computation, run detection, lift filtering, monthly breakdown
  AirTimeDetector.kt       Accelerometer-based jump detection
  Achievements.kt          Personal bests + achievement checking
  ResortDetector.kt        Nominatim reverse geocoding

  // Waypoints
  WaypointCard.kt          Detail card, edit sheet, delete sheet, coord header, hint capsule
  WaypointListSheet.kt     Sorted list with search
  WaypointIcons.kt         12 ski-themed preset icons

  // Sheets
  DailyStatsSheet.kt       Today + All-Time tabs, pace chart, personal bests, monthly
  TrackDetailSheet.kt      Full track breakdown with elevation profile and runs list
  SpeedometerSheet.kt      Full-screen live dashboard
  WeatherForecastSheet.kt  Current conditions + 24h hourly forecast
  SettingsSheet.kt         All user preferences + About + easter egg
  OnboardingOverlay.kt     First-launch 5-page swipeable tutorial

  // Data
  GpxHelper.kt             GPX import/export with XmlPullParser
  BackupHelper.kt          Zip backup/restore of all app data
  ShareImage.kt            Canvas-rendered PNG stats card + FileProvider share
  CoordinateFormat.kt      Decimal and DMS coordinate formatting

  // Weather
  WeatherHelper.kt         Open-Meteo current + forecast fetch

  // Widget
  StatsWidget.kt           Home screen widget showing today's runs + vertical
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Map | osmdroid + OpenStreetMap + OpenSnowMap + OpenTopoMap + CartoDB + Esri |
| GPS | Google Play Services Location + foreground service |
| Sensors | `Sensor.TYPE_LINEAR_ACCELERATION` (air time detection) |
| Weather | Open-Meteo (free, no key) |
| Geocoding | OpenStreetMap Nominatim |
| Persistence | SharedPreferences + Gson |
| Widgets | AppWidgetProvider + RemoteViews |

## Getting Started

### Prerequisites

- Android Studio (or the Android SDK)
- JDK 17 or 21 (Gradle's embedded Kotlin DSL is not compatible with JDK 25+)

### Build

```bash
./gradlew assembleDebug
```

### Install on a connected device

```bash
./gradlew installDebug
```

## Privacy

Piste is designed to respect your privacy:

- **Location** is used only to show your position, calculate distances, record tracks, and fetch weather. Your precise location never leaves the device (only your approximate location is sent to Open-Meteo and Nominatim for weather and resort name).
- **Photos** attached to waypoints are stored locally in the app's internal storage. They are never uploaded.
- **All your data** — waypoints, tracks, settings — lives in SharedPreferences on your device. There is no account, no cloud, no analytics, and no tracking.
- **Map tiles and weather** are loaded from public OSM-based providers: OpenStreetMap, OpenSnowMap, OpenTopoMap, CartoDB, Esri, and Open-Meteo.

The full privacy policy is available at [florianbraun05.github.io/Waypoint](https://florianbraun05.github.io/Waypoint/).

## License

MIT License. See [LICENSE](LICENSE) for details.
