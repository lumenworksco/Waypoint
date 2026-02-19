import SwiftUI

struct WaypointDetailCard: View {
    let waypoint: WaypointModel
    let distance: String
    let onDelete: () -> Void
    let onClose: () -> Void
    var onUpdate: ((String, String) -> Void)? = nil
    
    @State private var isEditing = false
    @State private var editName: String = ""
    @State private var editNotes: String = ""
    @FocusState private var focusedField: Field?
    private enum Field: Hashable { case name, notes }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if isEditing {
                VStack(alignment: .leading, spacing: 8) {
                    TextField("Name", text: $editName)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled(false)
                        .font(.subheadline)
                        .padding(8)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .focused($focusedField, equals: .name)
                        .submitLabel(.next)
                        .onSubmit { focusedField = .notes }
                    TextField("Notes (optional)", text: $editNotes, axis: .vertical)
                        .lineLimit(1...3)
                        .font(.subheadline)
                        .padding(8)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .focused($focusedField, equals: .notes)
                        .submitLabel(.done)
                        .onSubmit { focusedField = nil }
                    HStack(spacing: 10) {
                        Button("Cancel") {
                            withAnimation(.snappy) { isEditing = false }
                        }
                        .font(.subheadline)
                        Button("Save") { saveEdits() }
                        .font(.subheadline.weight(.semibold))
                        .buttonStyle(.borderedProminent)
                        .disabled(editName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            } else {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(waypoint.name)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                        if !waypoint.notes.isEmpty {
                            Text(waypoint.notes)
                                .font(.caption2)
                                .foregroundColor(.gray)
                        }
                        if !distance.isEmpty {
                            Text(distance)
                                .font(.caption2)
                                .foregroundColor(.gray)
                        }
                    }
                    Spacer()
                    Button(action: {
                        withAnimation(.snappy) {
                            isEditing = true
                            editName = waypoint.name
                            editNotes = waypoint.notes
                            focusedField = .name
                        }
                    }) {
                        Image(systemName: "pencil")
                            .font(.subheadline)
                            .padding(8)
                            .background(Color.gray.opacity(0.2))
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Edit waypoint")
                    Button(action: onDelete) {
                        Image(systemName: "trash.fill")
                            .font(.subheadline)
                            .padding(8)
                            .background(Color.red)
                            .foregroundColor(.white)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Delete waypoint")
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.caption2.weight(.semibold))
                            .padding(8)
                            .background(Color.gray.opacity(0.2))
                            .foregroundColor(.secondary)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Close details")
                }
            }
        }
        .padding(12)
        .contentShape(Rectangle())
        .onTapGesture {
            if isEditing { focusedField = nil }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                if isEditing {
                    if focusedField == .name {
                        Button("Next") { focusedField = .notes }
                    } else if focusedField == .notes {
                        Button("Save") { saveEdits() }
                        .disabled(!Validation.isNonEmpty(editName))
                    } else {
                        Button("Done") { focusedField = nil }
                    }
                }
            }
        }
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.1), radius: 8, x: 0, y: 2)
        .padding(.horizontal)
    }

    private func saveEdits() {
        let trimmedName = Validation.trimmed(editName)
        let trimmedNotes = Validation.trimmed(editNotes)
        guard Validation.isNonEmpty(trimmedName) else { return }
        focusedField = nil
        isEditing = false
        Haptics.success()
        // Defer the update to the next run loop so isEditing = false
        // is fully committed before the @Published re-render fires.
        DispatchQueue.main.async {
            onUpdate?(trimmedName, trimmedNotes)
        }
    }
}

#Preview("Waypoint Detail Card - Editing") {
    let sample = WaypointModel(
        id: UUID(),
        name: "Sample Point",
        notes: "A note",
        latitude: 37.332,
        longitude: -122.011,
        timestamp: Date()
    )
    WaypointDetailCard(
        waypoint: sample,
        distance: "120 m",
        onDelete: {},
        onClose: {},
        onUpdate: { _, _ in }
    )
}
