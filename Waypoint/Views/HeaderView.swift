import SwiftUI
import CoreLocation

struct HeaderView: View {
    @ObservedObject var weather: WeatherManager
    @ObservedObject var location: LocationManager
    let onTap: () -> Void
    
    var body: some View {
        HStack {
            Group {
                if let w = weather.weather {
                    HStack(spacing: 12) {
                        Image(systemName: weather.icon(for: w.weather.first?.icon ?? ""))
                            .font(.system(size: 28))
                            .foregroundColor(.blue)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(Int(w.main.temp))°C")
                                .font(.headline)
                                .fontWeight(.semibold)
                            Text(w.weather.first?.description.capitalized ?? "")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                    .padding(10)
                    .background(.ultraThinMaterial)
                    .cornerRadius(10)
                } else {
                    HStack(spacing: 12) {
                        Image(systemName: weather.isLoading ? "hourglass.circle" : "cloud")
                            .font(.system(size: 28))
                            .foregroundColor(.blue)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(weather.isLoading ? "Loading..." : "Weather Info")
                                .font(.headline)
                                .fontWeight(.semibold)
                            Text(location.userLocation != nil ? "Waiting for data" : "Waiting for location")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                    .padding(10)
                    .background(.ultraThinMaterial)
                    .cornerRadius(10)
                }
            }
            
            Spacer()
        }
        .padding()
    }
}

