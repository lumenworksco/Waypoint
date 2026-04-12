# Piste 2.1.4 — Release Notes

## What's New

**Piste** is your ski companion — built for the mountain, designed to feel at home in your hand.

### Track Every Run
Record your ski day with a single tap. Piste automatically detects individual runs, filters out lift rides, and calculates your vertical, max speed, distance, and time on snow. Watch your track painted on the map with a speed heatmap — blue for cruising, red for sending it.

### Activity Rings
Three daily goals — vertical, runs, and time — visualized as Apple Fitness-style rings that fill as you ski. When you exceed 100%, the ring wraps past the start with a glow. Close all three and you'll feel the celebration haptic.

### Live Dashboard
Tap the location header for a full-screen speedometer showing your speed, altitude, vertical descended, and run count in real time. Set a speed alert threshold and your phone vibrates when you're pushing it.

### Weather & Safety
Current conditions and a 24-hour forecast powered by Open-Meteo — no API key, no account. A daylight countdown warns you when sunset is approaching. Avalanche awareness alerts flag dangerous conditions (heavy snowfall, wind loading, rapid temperature swings) and link directly to your local EAWS bulletin.

### Waypoints
Long-press anywhere to drop a pin. Choose from 12 ski-themed icons, 6 colors, add photos and notes. Set one as home and a compass arrow always points you back. Proximity alerts vibrate as you approach saved spots — filtered by direction so you only get notified for waypoints ahead of you.

### Session Recap
After a 45+ minute session, a recap card appears with your headline numbers and a share button that generates a clean stats card. One-tap Share to Strava exports your track as GPX directly to the Strava app.

### Memories
If you've attached photos to waypoints during the day, the recap offers a "View Memories" slideshow — Ken Burns zoom, stats overlay, auto-advancing. Your mountain day, cinematic.

### Night Skiing
Piste detects when you're skiing after sunset and suggests switching to the dark map. A moon-themed indicator shows you're in night skiing mode.

### 5 Map Styles
Standard, Piste (OpenSnowMap with color-coded runs and lifts), Terrain (topographic contours), Satellite (Esri imagery), and Dark. Download any area for offline use — ski resorts rarely have good signal.

### Built for Privacy
All data stays on your device. No accounts, no cloud, no analytics. Weather and map tiles are fetched from public APIs. Your location never leaves your phone except as an approximate coordinate for weather lookups.

---

**What's under the hood:**
- MVVM architecture (ViewModel + Repository + StateFlow)
- Thread-safe GPS recording with foreground service
- Direction-aware proximity alerts
- Crash recovery detection for interrupted recordings
- ConcurrentHashMap stats caching across 11 call sites
- @Immutable data classes for optimized Compose recomposition
- Swipe-to-delete with undo across all lists
- Spring physics on achievement banners and UI transitions
- Haptic feedback on every meaningful interaction
- Full GPX import/export and ZIP backup/restore
- Home screen widget (today or season stats)
- 46 source files, 7,800 lines, 17 unit tests

Crafted in the French Alps.
