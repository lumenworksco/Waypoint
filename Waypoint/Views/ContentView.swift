import SwiftUI
import MapKit

// Helper extension to make CLLocationCoordinate2D equatable
extension CLLocationCoordinate2D: Equatable {
    public static func == (lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
        lhs.latitude == rhs.latitude && lhs.longitude == rhs.longitude
    }
}

struct ContentView: View {
    @StateObject private var locationMgr = LocationManager()
    @StateObject private var weatherMgr = WeatherManager()
    @StateObject private var waypointMgr = WaypointManager()
    
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 20, longitude: 0),
        span: MKCoordinateSpan(latitudeDelta: 100, longitudeDelta: 100)
    )
    @State private var mapType: MapDisplayType = .standard
    @State private var selected: WaypointModel?
    @State private var distance = ""
    @State private var newName = ""
    @State private var newNotes = ""
    @State private var showAdd = false
    @State private var showList = false
    @State private var showWeather = false
    @State private var showMapType = false
    
    var body: some View {
        ZStack {
            OSMMapView(
                region: $region,
                mapType: $mapType,
                userLocation: locationMgr.userLocation,
                waypoints: waypointMgr.waypoints,
                onTap: { wp in
                    selected = wp
                    if let loc = locationMgr.userLocation {
                        distance = waypointMgr.distance(from: loc, to: wp)
                    }
                }
            )
            .ignoresSafeArea()
            
            VStack {
                HeaderView(weather: weatherMgr, location: locationMgr, onTap: { showWeather.toggle() })
                Spacer()
                
                if let wp = selected {
                    WaypointDetailCard(
                        waypoint: wp,
                        distance: distance,
                        onNavigate: { navigate(to: wp) },
                        onDelete: { waypointMgr.delete(wp); selected = nil },
                        onClose: { withAnimation { selected = nil } }
                    )
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                
                ControlButtons(
                    waypointCount: waypointMgr.waypoints.count,
                    hasLocation: locationMgr.userLocation != nil,
                    onList: { showList.toggle() },
                    onMapType: { showMapType.toggle() },
                    onAdd: {
                        newName = "Waypoint \(waypointMgr.waypoints.count + 1)"
                        newNotes = ""
                        showAdd.toggle()
                    },
                    onCenter: centerOnUser
                )
            }
        }
        .onAppear { locationMgr.start() }
        .onChange(of: locationMgr.userLocation) { _, loc in
            if let loc = loc { weatherMgr.fetch(for: loc) }
        }
        .sheet(isPresented: $showAdd) {
            AddWaypointSheet(
                name: $newName,
                notes: $newNotes,
                location: locationMgr.userLocation,
                onSave: {
                    if let loc = locationMgr.userLocation {
                        waypointMgr.add(name: newName, coord: loc, notes: newNotes)
                    }
                    showAdd = false
                },
                onCancel: { showAdd = false }
            )
        }
        .sheet(isPresented: $showList) {
            WaypointsListSheet(
                waypoints: waypointMgr.waypoints,
                onTap: { navigate(to: $0); showList = false },
                onDelete: waypointMgr.delete,
                onDismiss: { showList = false }
            )
        }
        .sheet(isPresented: $showWeather) {
            WeatherDetailSheet(
                weather: weatherMgr.weather,
                icon: weatherMgr.icon(for: weatherMgr.weather?.weather.first?.icon ?? ""),
                onDismiss: { showWeather = false }
            )
        }
        .sheet(isPresented: $showMapType) {
            MapTypeSelectorSheet(selected: $mapType, onDismiss: { showMapType = false })
        }
    }
    
    private func centerOnUser() {
        guard let loc = locationMgr.userLocation else { return }
        withAnimation {
            region = MKCoordinateRegion(
                center: loc,
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            )
        }
    }
    
    private func navigate(to wp: WaypointModel) {
        withAnimation {
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
