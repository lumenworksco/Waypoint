import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';

import '../theme/app_theme.dart';

class MapHeader extends StatelessWidget {
  const MapHeader({
    super.key,
    required this.userLocation,
    required this.locationEnabled,
  });

  final LatLng? userLocation;
  final bool locationEnabled;

  @override
  Widget build(BuildContext context) {
    final coordText = userLocation != null
        ? '${userLocation!.latitude.toStringAsFixed(4)},  ${userLocation!.longitude.toStringAsFixed(4)}'
        : 'Locating\u2026';

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Align(
        alignment: Alignment.topLeft,
        child: ClipRRect(
          borderRadius: BorderRadius.circular(AppRadii.header),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 15, sigmaY: 15),
            child: Semantics(
              label: locationEnabled ? 'Location: $coordText' : 'Locating',
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 7,
                ),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.92),
                  borderRadius: BorderRadius.circular(AppRadii.header),
                  boxShadow: AppShadows.header,
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 7,
                      height: 7,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: locationEnabled
                            ? AppColors.iosBlue
                            : AppColors.statusDotInactive,
                      ),
                    ),
                    const SizedBox(width: 7),
                    Text(
                      coordText,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: locationEnabled
                            ? AppColors.textPrimary
                            : AppColors.textSecondary,
                        letterSpacing: 0.2,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
