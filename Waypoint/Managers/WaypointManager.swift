import Foundation
import CoreLocation
import Combine
import SwiftUI

class WaypointManager: ObservableObject {
    @Published var waypoints: [WaypointModel] = []
    private let saveKey = "SavedWaypoints"
    
    init() {
        load()
    }
    
    func add(name: String, coord: CLLocationCoordinate2D, notes: String) {
        waypoints.append(WaypointModel(name: name, coordinate: coord, notes: notes))
        save()
    }
    
    func delete(_ waypoint: WaypointModel) {
        waypoints.removeAll { $0.id == waypoint.id }
        save()
    }
    
    func delete(at offsets: IndexSet) {
        waypoints.remove(atOffsets: offsets)
        save()
    }
    
    func update(_ waypoint: WaypointModel, name: String, notes: String) {
        if let idx = waypoints.firstIndex(where: { $0.id == waypoint.id }) {
            waypoints[idx].name = name
            waypoints[idx].notes = notes
            save()
        }
    }
    
    func distance(from userLoc: CLLocationCoordinate2D, to waypoint: WaypointModel) -> String {
        let user = CLLocation(latitude: userLoc.latitude, longitude: userLoc.longitude)
        let dest = CLLocation(latitude: waypoint.coordinate.latitude, longitude: waypoint.coordinate.longitude)
        let dist = user.distance(from: dest)
        
        return dist < 1000 ? "\(Int(dist))m away" : String(format: "%.2fkm away", dist / 1000)
    }
    
    private func save() {
        if let data = try? JSONEncoder().encode(waypoints) {
            UserDefaults.standard.set(data, forKey: saveKey)
        }
    }
    
    private func load() {
        if let data = UserDefaults.standard.data(forKey: saveKey),
           let decoded = try? JSONDecoder().decode([WaypointModel].self, from: data) {
            waypoints = decoded
        }
    }
}
