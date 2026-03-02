import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

class HintCapsule extends StatelessWidget {
  const HintCapsule({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.65),
        borderRadius: BorderRadius.circular(AppRadii.capsule),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.pan_tool, color: Colors.white, size: 14),
          SizedBox(width: 6),
          Text(
            'Long-press to add',
            style: TextStyle(color: Colors.white, fontSize: 13),
          ),
        ],
      ),
    );
  }
}
