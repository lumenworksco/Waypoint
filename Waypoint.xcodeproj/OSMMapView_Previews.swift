import SwiftUI
import MapKit

#if DEBUG
struct OSMMapView_Previews: PreviewProvider {
    static var region = Binding.constant(MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 37.3349, longitude: -122.0090),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    ))

    static let waypoints: [WaypointModel] = [
        WaypointModel(name: "Apple Park", latitude: 37.3349, longitude: -122.0090, notes: "HQ"),
        WaypointModel(name: "Visitor Center", latitude: 37.3343, longitude: -122.0090)
    ]

    static var previews: some View {
        OSMMapView(
            region: region,
            userLocation: CLLocationCoordinate2D(latitude: 37.3349, longitude: -122.0090),
            waypoints: waypoints,
            onTap: { _ in },
            onLongPressAt: { _ in }
        )
        .frame(height: 300)
        .previewDisplayName("OSMMapView Preview")
    }
}
#endif
