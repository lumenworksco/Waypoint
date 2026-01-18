import Foundation
import CoreLocation
import Combine

class WeatherManager: ObservableObject {
    @Published var weather: WeatherResponse?
    @Published var isLoading = false
    
    private let apiKey = "YOUR_API_KEY_HERE"
    
    func fetch(for coord: CLLocationCoordinate2D) {
        isLoading = true
        let url = URL(string: "https://api.openweathermap.org/data/2.5/weather?lat=\(coord.latitude)&lon=\(coord.longitude)&appid=\(apiKey)&units=metric")!
        
        URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            DispatchQueue.main.async {
                self?.isLoading = false
                if let data = data, let weather = try? JSONDecoder().decode(WeatherResponse.self, from: data) {
                    self?.weather = weather
                }
            }
        }.resume()
    }
    
    func icon(for code: String) -> String {
        switch code {
        case "01d", "01n": return "sun.max.fill"
        case "02d", "02n": return "cloud.sun.fill"
        case "03d", "03n", "04d", "04n": return "cloud.fill"
        case "09d", "09n": return "cloud.rain.fill"
        case "10d", "10n": return "cloud.sun.rain.fill"
        case "11d", "11n": return "cloud.bolt.fill"
        case "13d", "13n": return "snow"
        case "50d", "50n": return "cloud.fog.fill"
        default: return "questionmark"
        }
    }
}
