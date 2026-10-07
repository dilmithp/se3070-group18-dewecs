import 'dart:io';

import 'package:flutter/material.dart';

import '../state/photo_picker.dart';
import '../strings.dart';

/// Camera or gallery, a preview and a remove button. The form owns the stored photo path.
class PhotoSection extends StatelessWidget {
  const PhotoSection({
    super.key,
    required this.photoPath,
    required this.busy,
    required this.onPick,
    required this.onRemove,
  });

  final String? photoPath;
  final bool busy;
  final ValueChanged<PhotoSource> onPick;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final path = photoPath;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(S.photoTitle, style: theme.textTheme.titleMedium),
        const SizedBox(height: 8),
        if (path != null) ...[
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Semantics(
              label: S.photoPreview,
              image: true,
              child: Image.file(
                File(path),
                height: 180,
                fit: BoxFit.cover,
                errorBuilder: (_, _, _) => const SizedBox(height: 180, child: Icon(Icons.broken_image, size: 48)),
              ),
            ),
          ),
          const SizedBox(height: 8),
          TextButton.icon(
            onPressed: onRemove,
            icon: const Icon(Icons.delete_outline),
            label: const Text(S.removePhoto),
          ),
        ] else
          Wrap(
            spacing: 8,
            children: [
              OutlinedButton.icon(
                onPressed: busy ? null : () => onPick(PhotoSource.camera),
                icon: const Icon(Icons.photo_camera),
                label: const Text(S.takePhoto),
              ),
              OutlinedButton.icon(
                onPressed: busy ? null : () => onPick(PhotoSource.gallery),
                icon: const Icon(Icons.photo_library),
                label: const Text(S.choosePhoto),
              ),
            ],
          ),
      ],
    );
  }
}
