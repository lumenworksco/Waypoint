import SwiftUI

struct ControlButtons: View {
    let hasLocation: Bool
    let onCenter: () -> Void
    
    var body: some View {
        HStack {
            Spacer()
            
            Button(action: onCenter) {
                Image(systemName: "location.fill")
                    .foregroundColor(.white)
                    .frame(width: 50, height: 50)
                    .background(Color.blue)
                    .clipShape(Circle())
            }
            .disabled(!hasLocation)
            .opacity(hasLocation ? 1 : 0.5)
            .accessibilityIdentifier("CenterButton")
        }
        .padding()
    }
}
