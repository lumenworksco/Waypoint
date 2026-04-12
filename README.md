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
- **Dark mode auto-switch** — follows the system theme; automatically switches to dark map tiles at night
- **Glare mode** — optional high-contrast black-on-white UI for bright snow conditions
- **Night skiing detection** — shows a moon-themed indicator when recording after sunset
- **Full edge-to-edge layout** — the map reaches from status bar to nav bar

### Run Tracking
- **Foreground service** — recording continues even when the phone is locked or in your pocket
- **Start, pause, resume, stop** — full control over each session
- **Auto-follow mode** — map keeps you centered while recording; disengages if you touch the map
- **Auto run detection** — splits your track into individual descents of 30m+ drop
- **Lift auto-pause** — speed on lifts is excluded from your max-speed stat automatically
- **Air time detection** — uses the accelerometer to detect jumps and measures their duration (opt-in via settings)
- **Track stats** — distance, duration, avg and max speed, vertical descended, elevation gain/loss
- **Per-run breakdown** — every detected run shown with difficulty (green/blue/black), drop, duration, and max speed
- **Time on snow vs lifts** — percentage of time actually skiing vs riding lifts
- **Elevation profile chart** — visual altitude graph for each recorded track
- **Speed heatmap** — selected track is drawn as a polyline colored by instantaneous speed
- **Auto resort naming** — new tracks are named with the detected resort and date
- **Track renaming** — edit any track's name after recording
- **Session recovery** — detects interrupted recordings and notifies you on next launch
- **GPS battery optimization** — reduces location frequency when recording is paused

### Live Dashboard
- **Location header** — shows place name, altitude, and live speed when moving
- **Speedometer sheet** — tap the header for a full-screen live view with huge speed, altitude, vertical, and run count
- **Weather pill** — current temperature, wind, and a high-wind warning when lifts may close
- **Home waypoint** — set any waypoint as "home" and get a compass-back pill showing direction and distance
- **Speed alerts** — configurable speed threshold with haptic vibration when exceeded

### Stats & History
- **Daily stats** — runs, vertical, max speed, distance, time, on-snow %, first/last session times, and a pace-of-day chart
- **Apple Fitness-style activity rings** — three daily goals (vertical, runs, time) with rings that wrap past 100% with a glow effect
- **Season calendar heatmap** — GitHub-contribution-style grid showing ski days at a glance
- **All-time stats** — lifetime totals plus personal bests (top speed, best day, most runs)
- **Monthly breakdown** — per-month totals sorted newest first
- **Streak counter** — consecutive days recorded, displayed prominently at 2+ days
- **Personal bests** — auto-tracked and celebrated with banner notifications and haptics when broken
- **Track comparison** — pick two tracks and see a row-by-row side-by-side comparison with the winner highlighted
- **Chairlift wait time estimation** — estimates average lift queue time from GPS data
- **Home screen widget** — today's runs and vertical (or season totals) on your launcher

### Waypoints
- **Long-press to add** — drop a waypoint anywhere on the map
- **12 ski-themed icons** — pin, lift, gondola, hut, food, first aid, powder, cliff, trees, warning, star, flag
- **6 preset colors** — organize your spots by category
- **Photos** — attach a gallery photo to any waypoint
- **Notes** — add context (50-char name limit, 500-char notes limit)
- **Search** — filter waypoints and tracks by name in the list sheet
- **Swipe to delete** — swipe left on any waypoint or track row to reveal delete
- **Distance display** — see how far you are from any selected waypoint
- **Share, Navigate, Set Home** — share coordinates, open directions in Google Maps, or mark as home
- **Proximity alerts** — direction-aware vibration when approaching a saved waypoint (filters to waypoints ahead of travel)
- **Undo delete** — animated toast with 5-second undo window and progress indicator

### Weather
- **Current conditions** — temperature, wind speed, weather code with animated emojis
- **24-hour forecast** — hourly temperature, conditions, and snowfall with refresh button
- **Snowfall total** — shows expected snowfall in the next 24 hours
- **High-wind warning** — pill turns red when winds reach lift-closure thresholds
- **Powered by Open-Meteo** — free, no API key required

### Safety
- **Daylight remaining** — pill appears when sunset is within 90 min; turns red under 30 min (SunCalc, no API)
- **Avalanche awareness** — pill appears when forecast shows heavy new snow, wind-loading, or rapid temperature swings; links to the official local EAWS bulletin
- **Notification controls** — pause/resume/stop the current recording directly from the persistent notification
- **Session recap** — celebratory card auto-shows after 45+ minute sessions with share and Share to Strava buttons
- **Memories slideshow** — Ken Burns photo slideshow of the day's waypoint photos

### Data
- **GPX import/export** — waypoints and tracks in the standard format
- **Share to Strava** — one-tap GPX share targeting Strava from the session recap
- **Full backup & restore** — zip of all data (waypoints, tracks, photos, settings)
- **Local storage only** — nothing in the cloud, no accounts

### Settings
- **Distance units** — Metric (km/m) or Imperial (mi/ft)
- **Default zoom** — starting zoom level (10-19)
- **Proximity alert radius** — 0-500m with direction-aware filtering
- **Speed alert threshold** — 0-120 km/h
- **Daily goals** — configurable vertical, runs, and time targets for activity rings
- **Glare mode** — high-contrast UI for bright snow
- **Auto dark map** — switches to dark map tiles with system dark mode
- **Air time detection** — toggle accelerometer-based jump detection
- **Keep screen on while recording** — stay awake during a session
- **Widget display mode** — choose between today's stats or season totals
- **Offline tile cache** — view size and clear
- **First-launch onboarding** — 5 swipeable cards explaining the core features

## Architecture

```
app/src/main/kotlin/com/florian/piste/
  MainActivity.kt          Entry point, edge-to-edge, ViewModel + theme
  PisteViewModel.kt        AndroidViewModel — location, weather, achievements state
  PisteRepository.kt       StateFlow-based data layer wrapping WaypointStore
  MapScreen.kt             Main screen — map, overlays, recording, sheet management
  Theme.kt                 Light, dark, and glare color schemes + iOS-style modifiers
  Constants.kt             All named constants (thresholds, intervals, limits)
  Waypoint.kt              @Immutable data classes: Waypoint, TrackPoint, Track
  WaypointStore.kt         SharedPreferences + Gson persistence
  DateUtil.kt              Shared start-of-day helper

  // Map
  MapTiles.kt              5 map styles and their tile sources
  MapOverlays.kt           Pin drawables, user dot, distance, bearing
  MapControls.kt           Compass, zoom pill, record button, scale bar
  MapPills.kt              Weather, daylight, avalanche, home arrow, night skiing pills
  MarkerCluster.kt         Grid-based marker clustering
  GlassModifier.kt         iOS-style glassmorphism modifiers
  TileCacheManager.kt      Offline tile cache size and cleanup

  // Tracking
  TrackRecorder.kt         Thread-safe recording state holder with crash recovery
  TrackingService.kt       Foreground service with pause/resume/stop notification
  TrackStats.kt            Stats computation, run detection, lift filtering, caching
  AirTimeDetector.kt       Accelerometer-based jump detection (opt-in)
  Achievements.kt          Personal bests + achievement detection
  ResortDetector.kt        Nominatim reverse geocoding with rate limiting
  RecordingBridge.kt       Bridge between notification actions and Compose state

  // Safety
  SunCalc.kt               Sunrise/sunset calculation (NOAA algorithm, no API)
  AvalancheAwareness.kt    Condition-based awareness + EAWS bulletin links
  AvalancheAwarenessSheet.kt  Detail sheet with safety advice

  // Waypoints
  WaypointCard.kt          Detail card, edit sheet, delete sheet, location header
  WaypointListSheet.kt     Searchable list with swipe-to-delete
  WaypointIcons.kt         12 ski-themed preset icons

  // Sheets
  DailyStatsSheet.kt       Today + All-Time tabs, activity rings, heatmap, breakdown
  TrackDetailSheet.kt      Track breakdown with elevation profile and rename
  TrackCompareSheet.kt     Side-by-side track comparison
  SessionRecapSheet.kt     End-of-day recap with share + Strava buttons
  SpeedometerSheet.kt      Full-screen live dashboard
  WeatherForecastSheet.kt  Current conditions + 24h forecast with refresh
  SettingsSheet.kt         All preferences + About + easter egg
  OnboardingOverlay.kt     First-launch tutorial
  MemoriesOverlay.kt       Ken Burns photo slideshow with stats
  ActivityRings.kt         Apple Fitness-style rings with 100%+ overflow glow

  // Data
  GpxHelper.kt             GPX import/export with color validation
  BackupHelper.kt          Zip backup/restore of all app data
  ShareImage.kt            PNG stats card + Strava GPX share
  AnimatedNumber.kt        Smooth number rollover animation

  // Weather
  WeatherHelper.kt         Open-Meteo current + forecast fetch

  // Widget
  StatsWidget.kt           Home screen widget (today or season mode)

  // Haptics
  Haptics.kt               Named haptic patterns (tap, pop, warning, celebration)
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM (AndroidViewModel + Repository + StateFlow) |
| Map | osmdroid + OpenStreetMap + OpenSnowMap + OpenTopoMap + CartoDB + Esri |
| GPS | Google Play Services Location + foreground service |
| Sensors | `Sensor.TYPE_LINEAR_ACCELERATION` (air time detection) |
| Weather | Open-Meteo (free, no key) |
| Geocoding | OpenStreetMap Nominatim |
| Persistence | SharedPreferences + Gson (via Repository) |
| Images | Coil |
| Widgets | AppWidgetProvider + RemoteViews |

## Getting Started

### Prerequisites

- Android Studio (or the Android SDK)
- JDK 17

### Build

```bash
./gradlew assembleDebug
```

### Run tests

```bash
./gradlew test
```

### Install on a connected device

```bash
./gradlew installDebug
```

### Release build

Configure your signing keystore, then:

```bash
./gradlew assembleRelease
```

See [Android signing docs](https://developer.android.com/studio/publish/app-signing) for keystore setup.

## Privacy

Piste is designed to respect your privacy:

- **Location** is used only to show your position, calculate distances, record tracks, and fetch weather. Your precise location never leaves the device (only your approximate location is sent to Open-Meteo and Nominatim for weather and resort name).
- **Photos** attached to waypoints are stored locally in the app's internal storage. They are never uploaded.
- **All your data** — waypoints, tracks, settings — lives on your device. There is no account, no cloud, no analytics, and no tracking.
- **Map tiles and weather** are loaded from public OSM-based providers and Open-Meteo.

The full privacy policy is available at the app's GitHub Pages site.

## License

MIT License. See [LICENSE](LICENSE) for details.
