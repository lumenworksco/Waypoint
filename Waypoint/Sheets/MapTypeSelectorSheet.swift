import SwiftUI

struct MapTypeSelectorSheet: View {
    @Binding var selected: MapDisplayType
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationView {
            List(MapDisplayType.allCases, id: \.self) { type in
                Button(action: { selected = type; onDismiss() }) {
                    HStack {
                        Text(type.rawValue)
                        Spacer()
                        if selected == type {
                            Image(systemName: "checkmark").foregroundColor(.blue)
                        }
                    }
                }
            }
            .navigationTitle("Map Type")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onDismiss)
                }
            }
        }
    }
}
