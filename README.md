# Waypoint

A cross-platform mobile app for dropping and managing geographical waypoints on OpenStreetMap. Built with Flutter and Dart, targeting both iOS and Android from a single codebase.

## Features

- **Interactive Map** — Full-screen OpenStreetMap with pinch-to-zoom and pan
- **Drop Waypoints** — Long-press anywhere on the map to place a new waypoint
- **Edit & Manage** — Tap a waypoint to view, edit its name/notes, or delete it
- **Live GPS Tracking** — Your position shown as a blue dot with real-time updates
- **Distance Display** — See how far you are from any selected waypoint
- **Auto-Center** — Map centers on your location when GPS first locks on
- **Offline Persistence** — Waypoints saved locally and survive app restarts
- **Haptic Feedback** — Tactile responses for add, select, and delete actions

## Architecture

```
lib/
├── main.dart                    # App entry point
├── theme/app_theme.dart         # Colors, radii, shadows
├── models/waypoint_model.dart   # Data model + JSON serialization
├── services/
│   ├── waypoint_repository.dart # SharedPreferences persistence
│   ├── location_service.dart    # GPS via Geolocator
│   └── haptic_service.dart      # Platform haptic feedback
├── utils/
│   ├── distance_util.dart       # Haversine distance + formatting
│   └── validators.dart          # Input sanitization
├── providers/
│   └── waypoint_provider.dart   # Riverpod state management
├── screens/
│   └── map_screen.dart          # Main map screen
└── widgets/
    ├── map_header.dart          # GPS status pill
    ├── hint_capsule.dart        # Long-press hint
    ├── recenter_button.dart     # Location recenter button
    ├── custom_map_marker.dart   # Teardrop pin painter
    └── waypoint_detail_card.dart# View/edit detail card
```

## Getting Started

### Prerequisites

- [Flutter SDK](https://docs.flutter.dev/get-started/install) (3.41+)
- Xcode 15+ (for iOS builds)
- Android Studio or Android SDK (for Android builds)

### Setup

```bash
git clone https://github.com/your-username/waypoint.git
cd waypoint
flutter pub get
```

### Run

```bash
# iOS Simulator
flutter run -d ios

# Android Emulator
flutter run -d android

# Connected device
flutter run
```

### Build for Release

```bash
# Android APK
flutter build apk --release

# Android App Bundle (for Play Store)
flutter build appbundle --release

# iOS (open in Xcode for archive/upload)
flutter build ios --release
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Flutter 3.41 |
| Language | Dart |
| State Management | Riverpod |
| Map | flutter_map + OpenStreetMap |
| GPS | Geolocator |
| Persistence | SharedPreferences |
| Coordinates | latlong2 |

## Privacy

Waypoint requests location permission solely to display your position on the map and calculate distances to your waypoints. Location data is processed on-device only and is never transmitted to any server. Waypoint data is stored locally on your device using SharedPreferences.

The app makes network requests only to load OpenStreetMap tile images.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
