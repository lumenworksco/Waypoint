import SwiftUI

struct WeatherDetailSheet: View {
    let weather: WeatherResponse?
    let icon: String
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                if let w = weather {
                    Image(systemName: icon)
                        .font(.system(size: 80))
                        .foregroundColor(.blue)
                    Text("\(Int(w.main.temp))°C")
                        .font(.system(size: 60, weight: .bold))
                    Text(w.weather.first?.description.capitalized ?? "")
                        .font(.title2)
                        .foregroundColor(.gray)
                    Text("Location: \(w.name)")
                        .font(.headline)
                        .foregroundColor(.gray)
                    
                    Divider().padding()
                    
                    VStack(spacing: 16) {
                        HStack {
                            Label("Feels Like", systemImage: "thermometer")
                            Spacer()
                            Text("\(Int(w.main.feelsLike))°C").fontWeight(.semibold)
                        }
                        HStack {
                            Label("Humidity", systemImage: "humidity")
                            Spacer()
                            Text("\(w.main.humidity)%").fontWeight(.semibold)
                        }
                        HStack {
                            Label("Wind", systemImage: "wind")
                            Spacer()
                            Text("\(w.wind.speed, specifier: "%.1f") m/s").fontWeight(.semibold)
                        }
                    }
                    .padding()
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(12)
                    .padding(.horizontal)
                }
                Spacer()
            }
            .padding()
            .navigationTitle(weather?.name.isEmpty == false ? weather!.name : "Weather Info")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { /* TODO: handle location action */ }) {
                        Image(systemName: "location")
                    }
                }
            }
        }
    }
}
