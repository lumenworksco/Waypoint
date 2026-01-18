import SwiftUI

struct WaypointDetailCard: View {
    let waypoint: WaypointModel
    let distance: String
    let onNavigate: () -> Void
    let onDelete: () -> Void
    let onClose: () -> Void
    var onUpdate: ((String, String) -> Void)? = nil
    
    @State private var isEditing = false
    @State private var editName: String = ""
    @State private var editNotes: String = ""
    @FocusState private var focusedField: Field?
    private enum Field: Hashable { case name, notes }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    if isEditing {
                        VStack(alignment: .leading, spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Name")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                TextField("Enter a name", text: $editName)
                                    .textInputAutocapitalization(.words)
                                    .autocorrectionDisabled(false)
                                    .padding(10)
                                    .background(Color(.secondarySystemBackground))
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .focused($focusedField, equals: .name)
                                    .submitLabel(.next)
                                    .onSubmit { focusedField = .notes }
                                    .scaleEffect(focusedField == .name ? 1.02 : 1.0)
                                    .animation(.snappy(duration: 0.15), value: focusedField)
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Notes")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                TextField("Optional notes", text: $editNotes, axis: .vertical)
                                    .lineLimit(1...4)
                                    .padding(10)
                                    .background(Color(.secondarySystemBackground))
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .focused($focusedField, equals: .notes)
                                    .submitLabel(.done)
                                    .onSubmit { focusedField = nil }
                                    .scaleEffect(focusedField == .notes ? 1.02 : 1.0)
                                    .animation(.snappy(duration: 0.15), value: focusedField)
                            }
                        }
                    } else {
                        Text(waypoint.name)
                            .font(.headline)
                            .fontWeight(.bold)
                        if !waypoint.notes.isEmpty {
                            Text(waypoint.notes)
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        if !distance.isEmpty {
                            Text(distance)
                                .font(.caption2)
                                .foregroundColor(.gray)
                        }
                    }
                }
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                        .font(.system(size: 24))
                        .accessibilityLabel("Close details")
                        .accessibilityHint("Dismisses the waypoint details card")
                }
            }
            
            HStack(spacing: 12) {
                if isEditing {
                    Button("Cancel") {
                        withAnimation(.snappy) { isEditing = false }
                    }
                    .accessibilityLabel("Cancel edit")
                    .accessibilityHint("Discard changes to this waypoint")
                    Button("Save") {
                        let trimmedName = editName.trimmingCharacters(in: .whitespacesAndNewlines)
                        let trimmedNotes = editNotes.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmedName.isEmpty else { return }
                        onUpdate?(trimmedName, trimmedNotes)
                        let _notify = UINotificationFeedbackGenerator()
                        _notify.notificationOccurred(.success)
                        focusedField = nil
                        withAnimation(.snappy) { isEditing = false }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(editName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityLabel("Save changes")
                    .accessibilityHint("Apply changes to this waypoint")
                } else {
                    Button(action: onNavigate) {
                        Label("Navigate", systemImage: "arrow.triangle.turn.up.right.circle.fill")
                            .font(.caption)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .accessibilityLabel("Navigate to waypoint")
                            .accessibilityHint("Centers the map on this waypoint")
                    }
                    
                    Button(action: {
                        withAnimation(.snappy) {
                            isEditing = true
                            editName = waypoint.name
                            editNotes = waypoint.notes
                            focusedField = .name
                        }
                    }) {
                        Label("Edit", systemImage: "pencil")
                            .font(.caption)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.gray.opacity(0.2))
                            .foregroundColor(.primary)
                            .cornerRadius(8)
                            .accessibilityLabel("Edit waypoint")
                            .accessibilityHint("Edit the name and notes for this waypoint")
                    }
                    
                    Button(action: onDelete) {
                        Label("Delete", systemImage: "trash.fill")
                            .font(.caption)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.red)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .accessibilityLabel("Delete waypoint")
                            .accessibilityHint("Removes this waypoint from the list")
                    }
                }
            }
        }
        .padding()
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                if isEditing {
                    Button("Done") { focusedField = nil }
                }
            }
        }
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: Color.black.opacity(0.1), radius: 12, x: 0, y: 4)
        .padding()
    }
}
