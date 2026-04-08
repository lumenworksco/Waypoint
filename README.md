# Waypoint

An Android app for dropping and managing geographical waypoints on OpenStreetMap. Built with Kotlin and Jetpack Compose.

## Features

- **Interactive Map** -- Full-screen OpenStreetMap with pinch-to-zoom and pan
- **Drop Waypoints** -- Long-press anywhere on the map to place a new waypoint
- **Edit & Manage** -- Tap a waypoint to view, edit its name/notes, or delete it
- **Live GPS Tracking** -- Your position shown as a blue dot with real-time updates
- **Distance Display** -- See how far you are from any selected waypoint
- **Auto-Center** -- Map centers on your location when GPS first locks on
- **Offline Persistence** -- Waypoints saved locally and survive app restarts
- **Haptic Feedback** -- Tactile responses for add, select, and delete actions

## Architecture

```
app/src/main/kotlin/com/florian/waypoint/
  MainActivity.kt      -- Entry point, edge-to-edge setup, Compose theme
  MapScreen.kt         -- Main screen: osmdroid map + Compose overlays
  Waypoint.kt          -- Data class with UUID, coordinates, name, notes
  WaypointStore.kt     -- SharedPreferences + Gson persistence
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Map | osmdroid + OpenStreetMap |
| GPS | Google Play Services Location |
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

Waypoint requests location permission solely to display your position on the map and calculate distances to your waypoints. Location data is processed on-device only and is never transmitted to any server. Waypoint data is stored locally on your device using SharedPreferences.

The app makes network requests only to load OpenStreetMap tile images.

The full privacy policy is available at [florianbraun05.github.io/Waypoint](https://florianbraun05.github.io/Waypoint/).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
