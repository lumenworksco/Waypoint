import 'package:shared_preferences/shared_preferences.dart';
import '../models/waypoint_model.dart';

class WaypointRepository {
  static const _key = 'waypoints_json';

  Future<List<WaypointModel>> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw == null || raw.isEmpty) return [];
    try {
      return WaypointModel.decode(raw);
    } catch (_) {
      return [];
    }
  }

  Future<void> save(List<WaypointModel> waypoints) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, WaypointModel.encode(waypoints));
  }
}
