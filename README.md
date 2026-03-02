<<<<<<< HEAD
# Waypoint

A lightweight iOS app for dropping and managing waypoints on an OpenStreetMap-powered map.

## Features

- **OpenStreetMap tiles** — custom `MKTileOverlay` renders OSM map data instead of Apple Maps
- **Long-press to add** — drop a waypoint anywhere on the map with a long-press gesture
- **Tap to select** — tap any pin to view its name, notes, and distance from your current location
- **Inline editing** — rename waypoints and update notes directly from the detail card
- **Delete waypoints** — remove pins you no longer need
- **Live GPS location** — shows your position with the standard blue dot
- **Auto-center on launch** — map zooms to your location as soon as GPS locks
- **Re-center button** — one-tap button to snap back to your current location
- **Haptic feedback** — tactile response for add, select, edit, and delete actions
- **Persistent storage** — waypoints are saved to `UserDefaults` and restored on launch

## Requirements

- iOS 26.0+
- Xcode 26+
- Swift 5

## Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/your-username/Waypoint.git
   ```
2. Open `Waypoint.xcodeproj` in Xcode
3. Select a simulator or connected device
4. Build and run (**⌘R**)

> **Note:** Location permissions are requested at runtime. Allow "While Using the App" for full functionality.

## Project Structure

```
Waypoint/
├── WaypointApp.swift              # App entry point
├── Models/
│   └── WaypointModel.swift        # Codable waypoint data model
├── Managers/
│   ├── LocationManager.swift      # CLLocationManager wrapper
│   ├── WaypointManager.swift      # CRUD + persistence
│   └── Haptics.swift              # UIFeedbackGenerator helpers
├── Views/
│   ├── ContentView.swift          # Main map screen
│   ├── OSMMapView.swift           # MKMapView ↔ SwiftUI bridge
│   ├── HeaderView.swift           # Top status bar overlay
│   └── WaypointDetailCard.swift   # Bottom detail / edit card
├── Overlays/
│   └── OSMTileOverlay.swift       # OSM tile URL template
└── Helpers/
    └── Validation.swift           # Input validation utilities
```

## License

This project is provided as-is for personal use.
=======
# Waypoint — Android

An Android port of the iOS Waypoint app. A minimal, single-screen waypoint manager built on OpenStreetMap.

## Features

- Drop waypoints with a long press anywhere on the map
- View, edit, and delete waypoints via an inline card
- Live GPS location tracking with auto-center on first fix
- Persistent storage — waypoints survive app restarts
- Haptic feedback on interactions

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Map | OSMDroid 6.1.18 (OpenStreetMap / MAPNIK) |
| Architecture | MVVM (single ViewModel) |
| Persistence | DataStore + kotlinx.serialization |
| Location | FusedLocationProviderClient |
| Min SDK | 31 (Android 12) |
| Target SDK | 35 |

## Getting Started

1. Clone the repo
2. Open in Android Studio Hedgehog or later
3. Run on a device or emulator with API 31+

No API keys required — map tiles are served by OpenStreetMap.

## Permissions

| Permission | Reason |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS location for map centering |
| `ACCESS_COARSE_LOCATION` | Fallback network location |
| `INTERNET` | Fetching map tiles |
| `ACCESS_NETWORK_STATE` | OSMDroid tile availability check |
| `VIBRATE` | Haptic feedback |
>>>>>>> repoB/main
