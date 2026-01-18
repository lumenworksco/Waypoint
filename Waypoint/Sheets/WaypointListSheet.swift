import SwiftUI

struct WaypointsListSheet: View {
    let waypoints: [WaypointModel]
    let onTap: (WaypointModel) -> Void
    let onDelete: (IndexSet) -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationView {
            List {
                ForEach(waypoints) { wp in
                    Button(action: { onTap(wp) }) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(wp.name).font(.headline)
                            if !wp.notes.isEmpty {
                                Text(wp.notes).font(.caption).foregroundColor(.gray)
                            }
                            Text(wp.timestamp, style: .date).font(.caption2).foregroundColor(.gray)
                        }
                    }
                }
                .onDelete(perform: onDelete)
            }
            .navigationTitle("Waypoints (\(waypoints.count))")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onDismiss)
                }
            }
        }
    }
}
