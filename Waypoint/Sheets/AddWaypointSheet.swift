import SwiftUI
import CoreLocation

struct AddWaypointSheet: View {
    @Binding var name: String
    @Binding var notes: String
    let location: CLLocationCoordinate2D?
    let onSave: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationView {
            Form {
                Section("Details") {
                    TextField("Name", text: $name)
                    TextField("Notes (optional)", text: $notes)
                }
                if let loc = location {
                    Section("Location") {
                        Text("Lat: \(loc.latitude, specifier: "%.6f")")
                        Text("Lon: \(loc.longitude, specifier: "%.6f")")
                    }
                }
            }
            .navigationTitle("Add Waypoint")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save", action: onSave).disabled(name.isEmpty)
                }
            }
        }
    }
}
