import SwiftUI

#if DEBUG
struct WaypointApp_Previews: PreviewProvider {
    static var previews: some View {
        // If ContentView exists, preview it; otherwise preview a placeholder
        Group {
            ContentViewPlaceholder()
                .previewDisplayName("App Root Placeholder")
        }
    }
}

private struct ContentViewPlaceholder: View {
    var body: some View {
        Text("Waypoint Preview")
            .padding()
    }
}
#endif

// Note: Replace `ContentViewPlaceholder` with your real `ContentView` in previews when available.
