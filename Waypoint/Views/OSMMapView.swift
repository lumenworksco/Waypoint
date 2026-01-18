import SwiftUI
import MapKit

struct OSMMapView: UIViewRepresentable {
    @Binding var region: MKCoordinateRegion
    @Binding var mapType: MapDisplayType
    let userLocation: CLLocationCoordinate2D?
    let waypoints: [WaypointModel]
    let onTap: (WaypointModel) -> Void
    
    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        let overlay = OSMTileOverlay()
        overlay.canReplaceMapContent = true
        map.addOverlay(overlay, level: .aboveLabels)
        return map
    }
    
    func updateUIView(_ map: MKMapView, context: Context) {
        // Clamp zoom levels
        let minDelta: CLLocationDegrees = 0.00015
        let maxDelta: CLLocationDegrees = 180
        let clampedLatDelta = min(max(region.span.latitudeDelta, minDelta), maxDelta)
        let clampedLonDelta = min(max(region.span.longitudeDelta, minDelta), maxDelta)
        let clampedRegion = MKCoordinateRegion(
            center: region.center,
            span: MKCoordinateSpan(latitudeDelta: clampedLatDelta, longitudeDelta: clampedLonDelta)
        )
        // Update binding to keep state consistent
        region = clampedRegion
        map.setRegion(clampedRegion, animated: true)
        
        map.mapType = mapType == .standard ? .standard : mapType == .satellite ? .satellite : .hybrid
        
        map.removeAnnotations(map.annotations)
        
        if let loc = userLocation {
            let pin = MKPointAnnotation()
            pin.coordinate = loc
            pin.title = "Your Location"
            map.addAnnotation(pin)
        }
        
        for wp in waypoints {
            let pin = MKPointAnnotation()
            pin.coordinate = wp.coordinate
            pin.title = wp.name
            pin.subtitle = wp.notes
            map.addAnnotation(pin)
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, MKMapViewDelegate {
        var parent: OSMMapView
        
        init(_ parent: OSMMapView) {
            self.parent = parent
        }
        
        func mapView(_ map: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            overlay as? MKTileOverlay != nil ? MKTileOverlayRenderer(tileOverlay: overlay as! MKTileOverlay) : MKOverlayRenderer(overlay: overlay)
        }
        
        func mapView(_ map: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            let view = map.dequeueReusableAnnotationView(withIdentifier: "Pin") as? MKMarkerAnnotationView ?? MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: "Pin")
            view.annotation = annotation
            view.canShowCallout = true
            
            if annotation.title == "Your Location" {
                view.markerTintColor = .systemBlue
                view.glyphImage = UIImage(systemName: "location.fill")
            } else {
                view.markerTintColor = .systemRed
                view.glyphImage = UIImage(systemName: "mappin")
            }
            
            return view
        }
        
        func mapView(_ map: MKMapView, didSelect view: MKAnnotationView) {
            if let annotation = view.annotation,
               let wp = parent.waypoints.first(where: { $0.coordinate.latitude == annotation.coordinate.latitude && $0.coordinate.longitude == annotation.coordinate.longitude }) {
                parent.onTap(wp)
            }
        }
        
        func mapView(_ map: MKMapView, regionDidChangeAnimated animated: Bool) {
            let minDelta: CLLocationDegrees = 0.00015
            let maxDelta: CLLocationDegrees = 180
            let span = map.region.span
            let clampedLatDelta = min(max(span.latitudeDelta, minDelta), maxDelta)
            let clampedLonDelta = min(max(span.longitudeDelta, minDelta), maxDelta)
            parent.region = MKCoordinateRegion(center: map.region.center, span: MKCoordinateSpan(latitudeDelta: clampedLatDelta, longitudeDelta: clampedLonDelta))
        }
    }
}

