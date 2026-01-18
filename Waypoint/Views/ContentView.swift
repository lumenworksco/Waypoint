import SwiftUI
import MapKit
import UIKit

private enum ActiveSheet: Identifiable {
    case add
    var id: Int { hashValue }
}

struct ContentView: View {
    @StateObject private var locationMgr = LocationManager()
    @StateObject private var waypointMgr = WaypointManager()
    
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 20, longitude: 0),
        span: MKCoordinateSpan(latitudeDelta: 100, longitudeDelta: 100)
    )
    @State private var selected: WaypointModel?
    @State private var distance = ""
    @State private var newName = ""
    @State private var newNotes = ""
    @State private var activeSheet: ActiveSheet? = nil
    
    var body: some View {
        ZStack {
            OSMMapView(
                region: $region,
                userLocation: locationMgr.userLocation,
                waypoints: waypointMgr.waypoints,
                onTap: { wp in
                    selected = wp
                    if let loc = locationMgr.userLocation {
                        distance = waypointMgr.distance(from: loc, to: wp)
                    }
                    let _impact = UIImpactFeedbackGenerator(style: .light)
                    _impact.impactOccurred()
                },
                onLongPressAt: { coord in
                    newName = "Waypoint \(waypointMgr.waypoints.count + 1)"
                    newNotes = ""
                    // Add directly without sheet for smoothness
                    waypointMgr.add(name: newName, coord: coord, notes: newNotes)
                    let _notify = UINotificationFeedbackGenerator()
                    _notify.notificationOccurred(.success)
                }
            )
            .ignoresSafeArea()
            
            VStack {
                HeaderView(location: locationMgr)
                Spacer()
                
                if let wp = selected {
                    WaypointDetailCard(
                        waypoint: wp,
                        distance: distance,
                        onNavigate: { navigate(to: wp) },
                        onDelete: {
                            let _notify = UINotificationFeedbackGenerator()
                            _notify.notificationOccurred(.warning)
                            waypointMgr.delete(wp); selected = nil
                        },
                        onClose: { withAnimation(.snappy) { selected = nil } },
                        onUpdate: { name, notes in
                            waypointMgr.update(wp, name: name, notes: notes)
                        }
                    )
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                
                ControlButtons(
                    hasLocation: locationMgr.userLocation != nil,
                    onCenter: centerOnUser
                )
            }
        }
        // Start location updates when the view appears
        .onAppear { locationMgr.start() }
        
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .add:
                AddWaypointSheet(
                    name: $newName,
                    notes: $newNotes,
                    location: locationMgr.userLocation,
                    onSave: {
                        if let loc = locationMgr.userLocation {
                            waypointMgr.add(name: newName, coord: loc, notes: newNotes)
                        }
                        activeSheet = nil
                    },
                    onCancel: { activeSheet = nil }
                )
            }
        }
    }
    
    private func centerOnUser() {
        let _impact = UIImpactFeedbackGenerator(style: .light)
        _impact.impactOccurred()
        guard let loc = locationMgr.userLocation else { return }
        withAnimation(.snappy) {
            region = MKCoordinateRegion(
                center: loc,
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            )
        }
    }
    
    private func navigate(to wp: WaypointModel) {
        withAnimation(.snappy) {
            region = MKCoordinateRegion(
                center: wp.coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            )
            selected = wp
            if let loc = locationMgr.userLocation {
                distance = waypointMgr.distance(from: loc, to: wp)
            }
        }
    }
}

#Preview {
    ContentView()
}
