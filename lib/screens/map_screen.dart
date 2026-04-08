import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:latlong2/latlong.dart';

import '../providers/waypoint_provider.dart';
import '../services/haptic_service.dart';
import '../theme/app_theme.dart';
import '../widgets/custom_map_marker.dart';
import '../widgets/hint_capsule.dart';
import '../widgets/map_header.dart';
import '../widgets/recenter_button.dart';
import '../widgets/waypoint_detail_card.dart';

class MapScreen extends ConsumerStatefulWidget {
  const MapScreen({super.key});

  @override
  ConsumerState<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends ConsumerState<MapScreen> {
  final _mapController = MapController();

  @override
  void dispose() {
    _mapController.dispose();
    super.dispose();
  }

  void _handleRecenter(WaypointState state) {
    if (state.isRecenterRequested && state.userLocation != null) {
      _mapController.move(state.userLocation!, 18.5);
      ref.read(waypointProvider.notifier).recenterHandled();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(waypointProvider);

    // Listen for recenter requests
    ref.listen(waypointProvider.select((s) => s.isRecenterRequested), (
      prev,
      next,
    ) {
      if (next) _handleRecenter(ref.read(waypointProvider));
    });

    return Scaffold(
      body: Stack(
        children: [
          // ── Full-screen map ────────────────────────────────────
          FlutterMap(
            mapController: _mapController,
            options: MapOptions(
              initialCenter: const LatLng(51.5074, -0.1278),
              initialZoom: 15,
              minZoom: 3,
              maxZoom: 20,
              onLongPress: (tapPosition, latLng) {
                HapticService.success();
                ref.read(waypointProvider.notifier).addWaypoint(latLng);
              },
              onTap: (tapPosition, latLng) {
                ref.read(waypointProvider.notifier).selectWaypoint(null);
              },
            ),
            children: [
              TileLayer(
                urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'com.florian.waypoint',
              ),
              // User location blue dot
              if (state.userLocation != null)
                MarkerLayer(
                  markers: [
                    Marker(
                      point: state.userLocation!,
                      width: 36,
                      height: 36,
                      child: const _UserLocationDot(),
                    ),
                  ],
                ),
              // Waypoint markers
              MarkerLayer(
                markers: state.waypoints.map((wp) {
                  final isSelected = wp.id == state.selectedWaypoint?.id;
                  return Marker(
                    point: wp.coordinate,
                    width: 100,
                    height: 75,
                    alignment: Alignment.topCenter,
                    child: GestureDetector(
                      onTap: () {
                        HapticService.light();
                        ref.read(waypointProvider.notifier).selectWaypoint(wp);
                      },
                      child: TeardropMarker(
                        label: wp.name,
                        isSelected: isSelected,
                      ),
                    ),
                  );
                }).toList(),
              ),
            ],
          ),

          // ── Header (top, below status bar) ─────────────────────
          SafeArea(
            bottom: false,
            child: MapHeader(
              userLocation: state.userLocation,
              locationEnabled: state.locationEnabled,
            ),
          ),

          // ── Bottom row: card/hint (left) + recenter (right) ────
          Positioned(
            left: 16,
            right: 16,
            bottom: MediaQuery.of(context).padding.bottom + 16,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                // Left slot: detail card OR hint
                Expanded(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Hint capsule
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 220),
                        reverseDuration: const Duration(milliseconds: 180),
                        transitionBuilder: (child, animation) {
                          return SlideTransition(
                            position:
                                Tween<Offset>(
                                  begin: const Offset(0, 0.5),
                                  end: Offset.zero,
                                ).animate(
                                  CurvedAnimation(
                                    parent: animation,
                                    curve: Curves.easeOut,
                                  ),
                                ),
                            child: FadeTransition(
                              opacity: animation,
                              child: child,
                            ),
                          );
                        },
                        child: state.selectedWaypoint != null
                            ? WaypointDetailCard(
                                key: ValueKey(state.selectedWaypoint!.id),
                                waypoint: state.selectedWaypoint!,
                                userLocation: state.userLocation,
                                onDismiss: () {
                                  ref
                                      .read(waypointProvider.notifier)
                                      .selectWaypoint(null);
                                },
                                onDelete: () {
                                  HapticService.warning();
                                  ref
                                      .read(waypointProvider.notifier)
                                      .deleteWaypoint(
                                        state.selectedWaypoint!.id,
                                      );
                                },
                                onSave: (name, notes) {
                                  HapticService.success();
                                  ref
                                      .read(waypointProvider.notifier)
                                      .saveEdit(
                                        state.selectedWaypoint!.id,
                                        name,
                                        notes,
                                      );
                                },
                              )
                            : state.showHint
                            ? const HintCapsule(key: ValueKey('hint'))
                            : const SizedBox.shrink(key: ValueKey('empty')),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                // Right slot: recenter button
                RecenterButton(
                  enabled: state.locationEnabled,
                  onPressed: () {
                    HapticService.light();
                    ref.read(waypointProvider.notifier).requestRecenter();
                  },
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// User location dot — solid blue with white ring and faint glow. — solid blue with white ring and faint glow.
class _UserLocationDot extends StatelessWidget {
  const _UserLocationDot();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 36,
        height: 36,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.primaryBlue.withValues(alpha: 0.14),
        ),
        child: Center(
          child: Container(
            width: 22,
            height: 22,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.white,
            ),
            child: Center(
              child: Container(
                width: 16,
                height: 16,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.primaryBlue,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
