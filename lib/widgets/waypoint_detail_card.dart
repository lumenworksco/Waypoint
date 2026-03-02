import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';

import '../models/waypoint_model.dart';
import '../theme/app_theme.dart';
import '../utils/distance_util.dart';
import '../utils/validators.dart';

class WaypointDetailCard extends StatefulWidget {
  const WaypointDetailCard({
    super.key,
    required this.waypoint,
    required this.userLocation,
    required this.onDismiss,
    required this.onDelete,
    required this.onSave,
  });

  final WaypointModel waypoint;
  final LatLng? userLocation;
  final VoidCallback onDismiss;
  final VoidCallback onDelete;
  final void Function(String name, String notes) onSave;

  @override
  State<WaypointDetailCard> createState() => _WaypointDetailCardState();
}

class _WaypointDetailCardState extends State<WaypointDetailCard> {
  bool _isEditing = false;
  late TextEditingController _nameCtrl;
  late TextEditingController _notesCtrl;
  final _notesFocus = FocusNode();

  @override
  void initState() {
    super.initState();
    _nameCtrl = TextEditingController(text: widget.waypoint.name);
    _notesCtrl = TextEditingController(text: widget.waypoint.notes);
  }

  @override
  void didUpdateWidget(covariant WaypointDetailCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.waypoint.id != oldWidget.waypoint.id) {
      _isEditing = false;
      _nameCtrl.text = widget.waypoint.name;
      _notesCtrl.text = widget.waypoint.notes;
    }
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _notesCtrl.dispose();
    _notesFocus.dispose();
    super.dispose();
  }

  bool get _isSaveEnabled => Validators.isNonEmpty(_nameCtrl.text);

  void _startEditing() {
    setState(() {
      _nameCtrl.text = widget.waypoint.name;
      _notesCtrl.text = widget.waypoint.notes;
      _isEditing = true;
    });
  }

  void _cancelEditing() => setState(() => _isEditing = false);

  void _save() {
    if (!_isSaveEnabled) return;
    setState(() => _isEditing = false);
    widget.onSave(_nameCtrl.text, _notesCtrl.text);
  }

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(AppRadii.card),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Container(
          constraints: const BoxConstraints(minWidth: 240, maxWidth: 280),
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.97),
            borderRadius: BorderRadius.circular(AppRadii.card),
            boxShadow: AppShadows.card,
          ),
          child: _isEditing ? _buildEditMode() : _buildViewMode(),
        ),
      ),
    );
  }

  Widget _buildViewMode() {
    String? distanceText;
    if (widget.userLocation != null) {
      final metres = DistanceUtil.calculate(
        widget.userLocation!,
        widget.waypoint.coordinate,
      );
      distanceText = DistanceUtil.format(metres);
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                widget.waypoint.name,
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 15,
                  color: AppColors.textPrimary,
                ),
              ),
              if (widget.waypoint.notes.isNotEmpty)
                Text(
                  widget.waypoint.notes,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.textSecondary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              if (distanceText != null)
                Text(
                  distanceText,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.textSecondary,
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(width: 8),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _CardIconButton(
              icon: Icons.edit,
              backgroundColor: AppColors.buttonBackground,
              iconColor: AppColors.buttonIcon,
              onTap: _startEditing,
              semanticLabel: 'Edit',
            ),
            const SizedBox(width: 6),
            _CardIconButton(
              icon: Icons.delete,
              backgroundColor: AppColors.deleteRed,
              iconColor: Colors.white,
              onTap: widget.onDelete,
              semanticLabel: 'Delete',
            ),
            const SizedBox(width: 6),
            _CardIconButton(
              icon: Icons.close,
              backgroundColor: AppColors.buttonBackground,
              iconColor: AppColors.buttonIcon,
              onTap: widget.onDismiss,
              semanticLabel: 'Close',
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildEditMode() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          controller: _nameCtrl,
          decoration: InputDecoration(
            hintText: 'Name',
            hintStyle: const TextStyle(
              color: AppColors.textSecondary,
              fontSize: 14,
            ),
            filled: true,
            fillColor: AppColors.fieldBackground,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppRadii.field),
              borderSide: BorderSide.none,
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 10,
            ),
          ),
          style: const TextStyle(fontSize: 14),
          textCapitalization: TextCapitalization.words,
          textInputAction: TextInputAction.next,
          onSubmitted: (_) => _notesFocus.requestFocus(),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _notesCtrl,
          focusNode: _notesFocus,
          decoration: InputDecoration(
            hintText: 'Notes',
            hintStyle: const TextStyle(
              color: AppColors.textSecondary,
              fontSize: 13,
            ),
            filled: true,
            fillColor: AppColors.fieldBackground,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppRadii.field),
              borderSide: BorderSide.none,
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 10,
            ),
          ),
          style: const TextStyle(fontSize: 13),
          textCapitalization: TextCapitalization.sentences,
          textInputAction: TextInputAction.done,
          minLines: 1,
          maxLines: 3,
          onSubmitted: (_) => _save(),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            TextButton(
              onPressed: _cancelEditing,
              child: const Text(
                'Cancel',
                style: TextStyle(color: AppColors.textSecondary, fontSize: 14),
              ),
            ),
            const SizedBox(width: 4),
            GestureDetector(
              onTap: _isSaveEnabled ? _save : null,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 7,
                ),
                decoration: BoxDecoration(
                  color: _isSaveEnabled
                      ? AppColors.iosBlue
                      : AppColors.disabledButton,
                  borderRadius: BorderRadius.circular(AppRadii.field),
                ),
                child: const Text(
                  'Save',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _CardIconButton extends StatelessWidget {
  const _CardIconButton({
    required this.icon,
    required this.backgroundColor,
    required this.iconColor,
    required this.onTap,
    required this.semanticLabel,
  });

  final IconData icon;
  final Color backgroundColor;
  final Color iconColor;
  final VoidCallback onTap;
  final String semanticLabel;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: semanticLabel,
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          width: 32,
          height: 32,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: backgroundColor,
          ),
          child: Icon(icon, color: iconColor, size: 15),
        ),
      ),
    );
  }
}
