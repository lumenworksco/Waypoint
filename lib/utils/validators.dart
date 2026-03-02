abstract final class Validators {
  static String sanitizeName(String raw) {
    final trimmed = raw.trim();
    if (trimmed.isEmpty) return 'Unnamed Waypoint';
    return trimmed.length > 64 ? trimmed.substring(0, 64) : trimmed;
  }

  static bool isNonEmpty(String value) => value.trim().isNotEmpty;
}
