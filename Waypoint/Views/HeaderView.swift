import SwiftUI
import CoreLocation

struct HeaderView: View {
    @ObservedObject var location: LocationManager
    
    var body: some View {
        HStack {
            HStack(spacing: 12) {
                Image(systemName: location.userLocation != nil ? "location.fill" : "location")
                    .font(.system(size: 28))
                    .foregroundColor(.blue)
                VStack(alignment: .leading, spacing: 2) {
                    Text(location.userLocation != nil ? "Location Ready" : "Waiting for Location")
                        .font(.headline)
                        .fontWeight(.semibold)
                    if let loc = location.userLocation {
                        Text(String(format: "%.4f, %.4f", loc.latitude, loc.longitude))
                            .font(.caption)
                            .foregroundColor(.gray)
                            .accessibilityIdentifier("UserCoordinateLabel")
                    } else {
                        Text("Enable Location Services")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding(10)
            .background(.ultraThinMaterial)
            .cornerRadius(10)
            .accessibilityElement(children: .combine)
            .accessibilityLabel(location.userLocation != nil ? "Location Ready" : "Waiting for Location")
            .accessibilityHint(location.userLocation != nil ? String(format: "Latitude %.4f, Longitude %.4f", location.userLocation!.latitude, location.userLocation!.longitude) : "Enable Location Services")
            
            Spacer()
        }
        .padding()
        .accessibilityElement(children: .contain)
    }
}

