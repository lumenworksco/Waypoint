import SwiftUI
import MapKit

struct OSMMapView: UIViewRepresentable {
    @Binding var region: MKCoordinateRegion
    let userLocation: CLLocationCoordinate2D?
    let waypoints: [WaypointModel]
    let onTap: (WaypointModel) -> Void
    let onLongPressAt: (CLLocationCoordinate2D) -> Void
    
    private final class WaypointAnnotation: NSObject, MKAnnotation {
        let id: UUID
        dynamic var coordinate: CLLocationCoordinate2D
        var title: String?
        var subtitle: String?
        
        init(id: UUID, coordinate: CLLocationCoordinate2D, title: String?, subtitle: String?) {
            self.id = id
            self.coordinate = coordinate
            self.title = title
            self.subtitle = subtitle
            super.init()
        }
    }
    
    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        let overlay = OSMTileOverlay()
        overlay.canReplaceMapContent = true
        map.addOverlay(overlay, level: .aboveLabels)
        
        map.showsUserLocation = true
        map.userTrackingMode = .none
        
        let longPress = UILongPressGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.handleLongPress(_:)))
        map.addGestureRecognizer(longPress)
        
        return map
    }
    
    func updateUIView(_ map: MKMapView, context: Context) {
        // Only set region when it meaningfully differs from what the map already shows
        let current = map.region
        let latDiff = abs(current.center.latitude - region.center.latitude)
        let lonDiff = abs(current.center.longitude - region.center.longitude)
        let spanLatDiff = abs(current.span.latitudeDelta - region.span.latitudeDelta)
        let spanLonDiff = abs(current.span.longitudeDelta - region.span.longitudeDelta)
        if latDiff > 0.0001 || lonDiff > 0.0001 || spanLatDiff > 0.001 || spanLonDiff > 0.001 {
            map.setRegion(region, animated: true)
        }

        // Diff annotations to avoid removing and re-adding everything
        let existingByID = Dictionary(
            uniqueKeysWithValues: map.annotations.compactMap { $0 as? WaypointAnnotation }.map { ($0.id, $0) }
        )
        let newIDs = Set(waypoints.map(\.id))
        let existingIDs = Set(existingByID.keys)

        // Remove stale annotations
        for id in existingIDs.subtracting(newIDs) {
            if let ann = existingByID[id] { map.removeAnnotation(ann) }
        }
        // Add new annotations
        for wp in waypoints where !existingIDs.contains(wp.id) {
            let ann = WaypointAnnotation(id: wp.id, coordinate: wp.coordinate, title: wp.name, subtitle: wp.notes)
            map.addAnnotation(ann)
        }
        // Update existing annotations
        for wp in waypoints {
            if let ann = existingByID[wp.id] {
                ann.title = wp.name
                ann.subtitle = wp.notes
            }
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, MKMapViewDelegate {
        var parent: OSMMapView

        private var regionUpdateWorkItem: DispatchWorkItem?

        init(_ parent: OSMMapView) {
            self.parent = parent
        }
        
        func mapView(_ map: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            overlay as? MKTileOverlay != nil ? MKTileOverlayRenderer(tileOverlay: overlay as! MKTileOverlay) : MKOverlayRenderer(overlay: overlay)
        }
        
        func mapView(_ map: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MKUserLocation { return nil }
            
            let view = map.dequeueReusableAnnotationView(withIdentifier: "Pin") as? MKMarkerAnnotationView ?? MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: "Pin")
            view.annotation = annotation
            view.canShowCallout = true
            
            if let wa = annotation as? WaypointAnnotation {
                view.markerTintColor = .systemRed
                view.glyphImage = UIImage(systemName: "mappin")
                
                view.accessibilityLabel = wa.title ?? "Waypoint"
                view.accessibilityHint = "Double-tap to select waypoint"
                view.accessibilityTraits.insert(.button)
            } else if annotation.title == "Your Location" {
                view.markerTintColor = .systemBlue
                view.glyphImage = UIImage(systemName: "location.fill")
                
                view.accessibilityLabel = "Your Location"
                view.accessibilityHint = "Shows your current position"
                view.accessibilityTraits.insert(.staticText)
            } else {
                view.markerTintColor = .systemRed
                view.glyphImage = UIImage(systemName: "mappin")
            }
            
            return view
        }
        
        func mapView(_ map: MKMapView, didSelect view: MKAnnotationView) {
            guard let annotation = view.annotation else { return }
            if let wa = annotation as? WaypointAnnotation,
               let wp = parent.waypoints.first(where: { $0.id == wa.id }) {
                parent.onTap(wp)
                return
            }
            // Fallback to coordinate match (e.g., user location)
            if let wp = parent.waypoints.first(where: { $0.coordinate.latitude == annotation.coordinate.latitude && $0.coordinate.longitude == annotation.coordinate.longitude }) {
                parent.onTap(wp)
            }
        }
        
        func mapView(_ map: MKMapView, regionDidChangeAnimated animated: Bool) {
            regionUpdateWorkItem?.cancel()
            let item = DispatchWorkItem { [weak self] in
                guard let self = self else { return }
                self.parent.region = map.region
            }
            regionUpdateWorkItem = item
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.12, execute: item)
        }
        
        @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
            guard gesture.state == .began, let map = gesture.view as? MKMapView else { return }
            let point = gesture.location(in: map)
            let coord = map.convert(point, toCoordinateFrom: map)
            parent.onLongPressAt(coord)
        }
    }
}

