import SwiftUI

struct WaypointDetailCard: View {
    let waypoint: WaypointModel
    let distance: String
    let onNavigate: () -> Void
    let onDelete: () -> Void
    let onClose: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    // Weather info replacing waypoint text
                    Text("Weather at waypoint")
                        .font(.headline)
                        .fontWeight(.bold)
                    Text("Temp: --°  ·  Conditions: --")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                        .font(.system(size: 24))
                }
            }

            HStack(spacing: 12) {
                Button(action: onNavigate) {
                    Label("Navigate", systemImage: "arrow.triangle.turn.up.right.circle.fill")
                        .font(.caption)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }

                Button(action: onDelete) {
                    Label("Delete", systemImage: "trash.fill")
                        .font(.caption)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.red)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .cornerRadius(16)
        .padding()
    }
}
