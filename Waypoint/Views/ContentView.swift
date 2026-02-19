import SwiftUI
import MapKit

struct ContentView: View {
    @StateObject private var locationMgr = LocationManager()
    @StateObject private var waypointMgr = WaypointManager()

    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 20, longitude: 0),
        span: MKCoordinateSpan(latitudeDelta: 100, longitudeDelta: 100)
    )
    @State private var selectedID: UUID?
    @State private var distance = ""
    @State private var showLongPressTip: Bool = true
    @State private var hasCenteredOnUser = false

    /// Live lookup of the selected waypoint from the manager's array.
    private var selected: WaypointModel? {
        guard let id = selectedID else { return nil }
        return waypointMgr.waypoints.first { $0.id == id }
    }

    var body: some View {
        ZStack {
            OSMMapView(
                region: $region,
                userLocation: locationMgr.userLocation,
                waypoints: waypointMgr.waypoints,
                onTap: { wp in
                    withAnimation(.snappy) {
                        selectedID = wp.id
                    }
                    if let loc = locationMgr.userLocation {
                        distance = waypointMgr.distance(from: loc, to: wp)
                    }
                    Haptics.impact(.light)
                },
                onLongPressAt: { coord in
                    let name = "Waypoint \(waypointMgr.waypoints.count + 1)"
                    waypointMgr.add(name: name, coord: coord, notes: "")
                    withAnimation(.snappy) { showLongPressTip = false }
                    Haptics.success()
                }
            )
            .ignoresSafeArea()

            VStack {
                HeaderView(location: locationMgr)
                Spacer()

                HStack(alignment: .bottom) {
                    if let wp = selected {
                        WaypointDetailCard(
                            waypoint: wp,
                            distance: distance,
                            onDelete: {
                                Haptics.warning()
                                waypointMgr.delete(wp)
                                withAnimation(.snappy) { selectedID = nil }
                            },
                            onClose: { withAnimation(.snappy) { selectedID = nil } },
                            onUpdate: { name, notes in
                                waypointMgr.update(wp, name: name, notes: notes)
                            }
                        )
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                    } else if showLongPressTip {
                        HStack(spacing: 6) {
                            Image(systemName: "hand.point.up.left.fill")
                                .foregroundStyle(.white)
                                .font(.caption)
                            Text("Long‑press to add")
                                .foregroundStyle(.white)
                                .font(.caption)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(.black.opacity(0.7))
                        .clipShape(Capsule())
                        .allowsHitTesting(false)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    Spacer()

                    Button(action: centerOnUser) {
                        Image(systemName: "location.fill")
                            .foregroundColor(.white)
                            .frame(width: 50, height: 50)
                            .background(Color.blue)
                            .clipShape(Circle())
                    }
                    .disabled(locationMgr.userLocation == nil)
                    .opacity(locationMgr.userLocation != nil ? 1 : 0.5)
                    .accessibilityIdentifier("CenterButton")
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
            }
        }
        .onAppear { locationMgr.start() }
        .onChange(of: locationMgr.userLocation?.latitude) {
            guard !hasCenteredOnUser, let loc = locationMgr.userLocation else { return }
            hasCenteredOnUser = true
            withAnimation(.snappy) {
                region = MKCoordinateRegion(
                    center: loc,
                    span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                )
            }
        }
    }

    private func centerOnUser() {
        Haptics.impact(.light)
        guard let loc = locationMgr.userLocation else { return }
        withAnimation(.snappy) {
            region = MKCoordinateRegion(
                center: loc,
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            )
        }
    }
}

#Preview {
    ContentView()
}
