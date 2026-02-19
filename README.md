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
