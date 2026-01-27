import SwiftUI
import CoreLocation
import UIKit

// Lightweight helpers to make this view compile in isolation.
private enum Validation {
    static func isNonEmpty(_ text: String) -> Bool { !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}

private enum Haptics {
    static func success() {
        #if canImport(UIKit)
        import UIKit
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        #elseif canImport(AppKit)
        // Provide a light haptic on macOS when available; otherwise no-op
        NSHapticFeedbackManager.defaultPerformer.perform(.generic, performanceTime: .now)
        #else
        // Fallback no-op on other platforms
        #endif
    }
}

struct AddWaypointSheet: View {
    @Binding var name: String
    @Binding var notes: String
    let location: CLLocationCoordinate2D?
    let onSave: () -> Void
    let onCancel: () -> Void

    @FocusState private var focusedField: Field?
    private enum Field: Hashable { case name, notes }
    
    var body: some View {
        NavigationView {
            Form {
                Section("Details") {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Name")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        TextField("Name", text: $name)
                            .textInputAutocapitalization(.words)
                            .autocorrectionDisabled(false)
                            .padding(10)
                            .background(.thinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .focused($focusedField, equals: .name)
                            .submitLabel(.next)
                            .onSubmit { focusedField = .notes }
                            .scaleEffect(focusedField == .name ? 1.02 : 1.0)
                            .animation(.snappy(duration: 0.15), value: focusedField)
                            .accessibilityIdentifier("NameField")
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Notes")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        TextField("Notes (optional)", text: $notes, axis: .vertical)
                            .lineLimit(1...4)
                            .padding(10)
                            .background(.thinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .focused($focusedField, equals: .notes)
                            .submitLabel(.done)
                            .onSubmit { focusedField = nil }
                            .scaleEffect(focusedField == .notes ? 1.02 : 1.0)
                            .animation(.snappy(duration: 0.15), value: focusedField)
                            .accessibilityIdentifier("NotesField")
                    }
                }
                if let loc = location {
                    Section("Location") {
                        Text("Lat: \(loc.latitude, specifier: "%.6f")")
                        Text("Lon: \(loc.longitude, specifier: "%.6f")")
                    }
                }
            }
            .navigationTitle("Add Waypoint")
            .contentShape(Rectangle())
            .onTapGesture { focusedField = nil }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                        .accessibilityIdentifier("CancelAddButton")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        guard Validation.isNonEmpty(name) else { return }
                        Haptics.success()
                        focusedField = nil
                        onSave()
                    }
                    .disabled(!Validation.isNonEmpty(name))
                    .accessibilityIdentifier("SaveWaypointButton")
                }
            }
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    if focusedField == .name {
                        Button("Next") { focusedField = .notes }
                    } else if focusedField == .notes {
                        Button("Save") {
                            guard Validation.isNonEmpty(name) else { return }
                            Haptics.success()
                            focusedField = nil
                            onSave()
                        }
                        .disabled(!Validation.isNonEmpty(name))
                    } else {
                        Button("Done") { focusedField = nil }
                    }
                }
            }
        }
    }
}

#Preview("Add Waypoint Sheet") {
    struct Container: View {
        @State private var name = "Preview Name"
        @State private var notes = "Some notes"
        var body: some View {
            AddWaypointSheet(
                name: $name,
                notes: $notes,
                location: .init(latitude: 37.332, longitude: -122.011),
                onSave: {},
                onCancel: {}
            )
        }
    }
    return Container()
}
