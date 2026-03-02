import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

class RecenterButton extends StatelessWidget {
  const RecenterButton({
    super.key,
    required this.enabled,
    required this.onPressed,
  });

  final bool enabled;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: enabled ? 'Center on my location' : 'Location unavailable',
      child: GestureDetector(
        onTap: enabled ? onPressed : null,
        child: Container(
          width: 50,
          height: 50,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.white,
            boxShadow: enabled
                ? AppShadows.recenterEnabled
                : AppShadows.recenterDisabled,
          ),
          child: Icon(
            Icons.my_location,
            color: AppColors.iosBlue.withValues(alpha: enabled ? 1.0 : 0.4),
            size: 22,
          ),
        ),
      ),
    );
  }
}
