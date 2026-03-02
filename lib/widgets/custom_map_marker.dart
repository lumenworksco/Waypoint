import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

class TeardropMarker extends StatelessWidget {
  const TeardropMarker({
    super.key,
    required this.label,
    this.isSelected = false,
  });

  final String label;
  final bool isSelected;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: const Size(100, 75),
      painter: _TeardropPinPainter(label: label, isSelected: isSelected),
    );
  }
}

class _TeardropPinPainter extends CustomPainter {
  _TeardropPinPainter({required this.label, required this.isSelected});

  final String label;
  final bool isSelected;

  @override
  void paint(Canvas canvas, Size size) {
    final pinColor = isSelected ? AppColors.selectedPin : AppColors.charcoal;

    // Pin dimensions (matching Android's 32dp * density approach)
    const pinW = 32.0;
    const pinH = pinW * 1.35;
    const headR = pinW / 2;

    // ── Label pill ──────────────────────────────────────────────
    const textSizePx = 11.0;
    const pillPH = 5.0;
    const pillPV = 3.0;
    const pillCorner = 6.0;
    const pillGap = 3.0;

    final textPainter = TextPainter(
      text: TextSpan(
        text: label,
        style: const TextStyle(
          color: AppColors.textPrimary,
          fontSize: textSizePx,
          fontWeight: FontWeight.w500,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();

    final pillW = textPainter.width + pillPH * 2;
    final pillH = textPainter.height + pillPV * 2;

    // Centre everything horizontally
    final cx = size.width / 2;

    // ── Draw pill shadow ────────────────────────────────────────
    final pillRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(cx - pillW / 2, 0, pillW, pillH),
      const Radius.circular(pillCorner),
    );
    canvas.drawRRect(
      pillRect.shift(const Offset(0, 0.8)),
      Paint()..color = Colors.black.withValues(alpha: 0.11),
    );
    // White pill body
    canvas.drawRRect(
      pillRect,
      Paint()..color = Colors.white.withValues(alpha: 0.95),
    );
    // Label text
    textPainter.paint(canvas, Offset(cx - textPainter.width / 2, pillPV));

    // ── Teardrop pin with hole (EVEN_ODD) ───────────────────────
    final oy = pillH + pillGap;
    final headCy = oy + headR;
    final tipY = oy + pinH;

    final path = Path()..fillType = PathFillType.evenOdd;

    // Start at tip
    path.moveTo(cx, tipY);

    // Left side curve
    path.cubicTo(
      cx - headR * 0.15,
      tipY - pinH * 0.08,
      cx - headR,
      headCy + headR * 0.5,
      cx - headR,
      headCy,
    );

    // Top arc (semicircle left → right)
    path.arcTo(
      Rect.fromCircle(center: Offset(cx, headCy), radius: headR),
      math.pi, // start at 180°
      -math.pi, // sweep -180° (counterclockwise)
      false,
    );

    // Right side curve
    path.cubicTo(
      cx + headR,
      headCy + headR * 0.5,
      cx + headR * 0.15,
      tipY - pinH * 0.08,
      cx,
      tipY,
    );
    path.close();

    // Inner hole
    final holeCy = headCy - headR * 0.05;
    const holeR = headR * 0.38;
    path.addOval(Rect.fromCircle(center: Offset(cx, holeCy), radius: holeR));

    canvas.drawPath(
      path,
      Paint()
        ..color = pinColor
        ..style = PaintingStyle.fill
        ..isAntiAlias = true,
    );
  }

  @override
  bool shouldRepaint(covariant _TeardropPinPainter oldDelegate) =>
      label != oldDelegate.label || isSelected != oldDelegate.isSelected;
}
