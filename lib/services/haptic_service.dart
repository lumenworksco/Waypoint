import 'package:flutter/services.dart';

abstract final class HapticService {
  static void light() => HapticFeedback.lightImpact();
  static void success() => HapticFeedback.mediumImpact();
  static void warning() => HapticFeedback.heavyImpact();
}
