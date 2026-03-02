import 'dart:convert';

import 'package:latlong2/latlong.dart';
import 'package:uuid/uuid.dart';

const _uuid = Uuid();

class WaypointModel {
  WaypointModel({
    String? id,
    required this.name,
    required this.coordinate,
    DateTime? timestamp,
    this.notes = '',
  }) : id = id ?? _uuid.v4(),
       timestamp = timestamp ?? DateTime.now();

  final String id;
  final String name;
  final LatLng coordinate;
  final DateTime timestamp;
  final String notes;

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'latitude': coordinate.latitude,
    'longitude': coordinate.longitude,
    'timestamp': timestamp.millisecondsSinceEpoch,
    'notes': notes,
  };

  factory WaypointModel.fromJson(Map<String, dynamic> json) => WaypointModel(
    id: json['id'] as String,
    name: json['name'] as String,
    coordinate: LatLng(
      (json['latitude'] as num).toDouble(),
      (json['longitude'] as num).toDouble(),
    ),
    timestamp: DateTime.fromMillisecondsSinceEpoch(json['timestamp'] as int),
    notes: json['notes'] as String? ?? '',
  );

  WaypointModel copyWith({String? name, String? notes}) => WaypointModel(
    id: id,
    name: name ?? this.name,
    coordinate: coordinate,
    timestamp: timestamp,
    notes: notes ?? this.notes,
  );

  static String encode(List<WaypointModel> waypoints) =>
      jsonEncode(waypoints.map((w) => w.toJson()).toList());

  static List<WaypointModel> decode(String source) =>
      (jsonDecode(source) as List)
          .map((e) => WaypointModel.fromJson(e as Map<String, dynamic>))
          .toList();

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is WaypointModel &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
