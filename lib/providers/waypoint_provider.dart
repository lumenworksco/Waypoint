import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';

import '../models/waypoint_model.dart';
import '../services/location_service.dart';
import '../services/waypoint_repository.dart';
import '../utils/validators.dart';

class WaypointState {
  const WaypointState({
    this.waypoints = const [],
    this.userLocation,
    this.locationEnabled = false,
    this.selectedWaypoint,
    this.showHint = true,
    this.isRecenterRequested = false,
    this.hasCenteredOnUser = false,
  });

  final List<WaypointModel> waypoints;
  final LatLng? userLocation;
  final bool locationEnabled;
  final WaypointModel? selectedWaypoint;
  final bool showHint;
  final bool isRecenterRequested;
  final bool hasCenteredOnUser;

  WaypointState copyWith({
    List<WaypointModel>? waypoints,
    LatLng? userLocation,
    bool? locationEnabled,
    WaypointModel? selectedWaypoint,
    bool clearSelected = false,
    bool? showHint,
    bool? isRecenterRequested,
    bool? hasCenteredOnUser,
  }) {
    return WaypointState(
      waypoints: waypoints ?? this.waypoints,
      userLocation: userLocation ?? this.userLocation,
      locationEnabled: locationEnabled ?? this.locationEnabled,
      selectedWaypoint: clearSelected
          ? null
          : (selectedWaypoint ?? this.selectedWaypoint),
      showHint: showHint ?? this.showHint,
      isRecenterRequested: isRecenterRequested ?? this.isRecenterRequested,
      hasCenteredOnUser: hasCenteredOnUser ?? this.hasCenteredOnUser,
    );
  }
}

class WaypointNotifier extends StateNotifier<WaypointState> {
  WaypointNotifier(this._repository, this._locationService)
    : super(const WaypointState()) {
    _init();
  }

  final WaypointRepository _repository;
  final LocationService _locationService;
  StreamSubscription<Position>? _locationSub;

  Future<void> _init() async {
    final saved = await _repository.load();
    state = state.copyWith(waypoints: saved, showHint: saved.isEmpty);
    await _startLocation();
  }

  Future<void> _startLocation() async {
    final granted = await _locationService.requestPermission();
    if (!granted) {
      state = state.copyWith(locationEnabled: false);
      return;
    }
    _locationSub = _locationService.positionStream.listen(
      (position) {
        final latLng = LatLng(position.latitude, position.longitude);
        final shouldAutoCenter = !state.hasCenteredOnUser;
        state = state.copyWith(
          userLocation: latLng,
          locationEnabled: true,
          isRecenterRequested: shouldAutoCenter
              ? true
              : state.isRecenterRequested,
          hasCenteredOnUser: true,
        );
      },
      onError: (_) {
        state = state.copyWith(locationEnabled: false);
      },
    );
  }

  void addWaypoint(LatLng coordinate) {
    final newWaypoint = WaypointModel(
      name: 'Waypoint ${state.waypoints.length + 1}',
      coordinate: coordinate,
    );
    final updated = [...state.waypoints, newWaypoint];
    state = state.copyWith(waypoints: updated, showHint: false);
    _repository.save(updated);
  }

  void selectWaypoint(WaypointModel? waypoint) {
    if (waypoint == null) {
      state = state.copyWith(clearSelected: true);
    } else {
      state = state.copyWith(selectedWaypoint: waypoint);
    }
  }

  void saveEdit(String id, String name, String notes) {
    final sanitizedName = Validators.sanitizeName(name);
    final updated = state.waypoints.map((wp) {
      if (wp.id == id) {
        return wp.copyWith(name: sanitizedName, notes: notes.trim());
      }
      return wp;
    }).toList();
    final updatedSelected = updated.where((wp) => wp.id == id).firstOrNull;
    state = state.copyWith(
      waypoints: updated,
      selectedWaypoint: updatedSelected,
    );
    _repository.save(updated);
  }

  void deleteWaypoint(String id) {
    final updated = state.waypoints.where((wp) => wp.id != id).toList();
    state = state.copyWith(
      waypoints: updated,
      clearSelected: true,
      showHint: updated.isEmpty,
    );
    _repository.save(updated);
  }

  void requestRecenter() {
    state = state.copyWith(isRecenterRequested: true);
  }

  void recenterHandled() {
    state = state.copyWith(isRecenterRequested: false);
  }

  @override
  void dispose() {
    _locationSub?.cancel();
    super.dispose();
  }
}

final waypointRepositoryProvider = Provider((_) => WaypointRepository());
final locationServiceProvider = Provider((_) => LocationService());

final waypointProvider = StateNotifierProvider<WaypointNotifier, WaypointState>(
  (ref) {
    return WaypointNotifier(
      ref.watch(waypointRepositoryProvider),
      ref.watch(locationServiceProvider),
    );
  },
);
