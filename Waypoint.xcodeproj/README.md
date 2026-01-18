# Waypoint App

A simple waypoint app demonstrating an OSM-backed map, user location, and persistent waypoints.

## Features

- OSM map with region clamping and smooth sync to SwiftUI state
- Built-in user location indicator (no custom pin)
- Add waypoint via long-press on map
- Add waypoint via sheet (current location)
- Waypoint list with navigate, edit, delete
- Waypoint detail card with distance from current location
- Map type selector (Standard/Satellite/Hybrid)
- Persistence to UserDefaults
- Haptic feedback helpers
- Accessibility labels and identifiers on key controls
- Unit-style tests for distance formatting and persistence (Swift Testing)
- UI tests for list, add, map type change, and center-on-user

## Running UI Tests Deterministically

To ensure location-dependent UI tests pass reliably on simulator and CI:

1. Add the included GPX to your Run and UI Test schemes
   - Select the app scheme > Edit Scheme… > Run > Options
   - Check "Allow Location Simulation"
   - Choose `Resources/SimulatedLocation.gpx`
   - Repeat for the Test action if desired
2. Grant location permissions on first run when prompted.

## Project Structure

- `ContentView.swift`: Main screen, unified sheet state
- `Map/WaypointAnnotation.swift`: Custom MKAnnotation carrying waypoint ID
- `Map/OSMMapView.swift`: UIKit bridge for MKMapView + OSM tiles
- `Managers/LocationManager.swift`: User location + heading with auth handling
- `Managers/WaypointManager.swift`: Waypoint CRUD + persistence + distance
- `Models/WaypointModel.swift`: Codable, Identifiable waypoint model
- `Views/` (HeaderView, WaypointDetailCard, ControlButtons, sheets)
- `Utilities/Haptics.swift`: Haptic feedback helper
- `Extensions/CLLocationCoordinate2D+Equatable.swift`: Equality helper
- `Resources/SimulatedLocation.gpx`: Deterministic location for tests
- `Tests/WaypointManagerTests.swift`: Swift Testing examples
- `UITests/*.swift`: UI tests using XCTest

## Notes

- If your UI tests need to type into text fields, disable the hardware keyboard in the simulator (I/O > Keyboard > Connect Hardware Keyboard).
- For CI, consider increasing UI test timeouts or using expectations where appropriate.
