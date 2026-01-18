import Foundation
import CoreLocation

struct WaypointModel: Identifiable, Codable {
    let id: UUID
    var name: String
    let coordinate: CLLocationCoordinate2D
    let timestamp: Date
    var notes: String
    
    enum CodingKeys: String, CodingKey {
        case id, name, timestamp, notes, latitude, longitude
    }
    
    init(id: UUID = UUID(), name: String, coordinate: CLLocationCoordinate2D, timestamp: Date = Date(), notes: String = "") {
        self.id = id
        self.name = name
        self.coordinate = coordinate
        self.timestamp = timestamp
        self.notes = notes
    }
    
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        timestamp = try c.decode(Date.self, forKey: .timestamp)
        notes = try c.decode(String.self, forKey: .notes)
        let lat = try c.decode(Double.self, forKey: .latitude)
        let lon = try c.decode(Double.self, forKey: .longitude)
        coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
    }
    
    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(name, forKey: .name)
        try c.encode(timestamp, forKey: .timestamp)
        try c.encode(notes, forKey: .notes)
        try c.encode(coordinate.latitude, forKey: .latitude)
        try c.encode(coordinate.longitude, forKey: .longitude)
    }
}
