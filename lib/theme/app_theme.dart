import 'package:flutter/material.dart';

abstract final class AppColors {
  static const primaryBlue = Color(0xFF007AFF);
  static const charcoal = Color(0xFF3C3734);
  static const selectedPin = Color(0xFFDC5028);
  static const deleteRed = Color(0xFFFF3B30);
  static const textPrimary = Color(0xFF1C1C1E);
  static const textSecondary = Color(0xFF8E8E93);
  static const fieldBackground = Color(0xFFF2F2F7);
  static const buttonBackground = Color(0xFFEFEFF4);
  static const buttonIcon = Color(0xFF3C3C43);
  static const disabledButton = Color(0xFFD1D1D6);
  static const statusDotInactive = Color(0xFFAEAEB2);
}

abstract final class AppRadii {
  static const double field = 8;
  static const double header = 20;
  static const double card = 16;
  static const double capsule = 50;
  static const double pillLabel = 6;
}

abstract final class AppShadows {
  static List<BoxShadow> card = [
    BoxShadow(
      color: Colors.black.withValues(alpha: 0.1),
      blurRadius: 8,
      offset: const Offset(0, 2),
    ),
  ];

  static List<BoxShadow> header = [
    BoxShadow(color: Colors.black.withValues(alpha: 0.1), blurRadius: 8),
  ];

  static List<BoxShadow> recenterEnabled = [
    BoxShadow(color: Colors.black.withValues(alpha: 0.15), blurRadius: 6),
  ];

  static List<BoxShadow> recenterDisabled = [
    BoxShadow(color: Colors.black.withValues(alpha: 0.1), blurRadius: 2),
  ];
}
