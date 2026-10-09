import 'package:flutter/material.dart';

/// A white bordered card with an optional bold heading: the building block of every form and detail page,
/// the same as the web `.card`.
class SectionCard extends StatelessWidget {
  const SectionCard({super.key, this.title, this.icon, required this.child, this.padding = const EdgeInsets.all(16)});

  final String? title;
  final IconData? icon;
  final Widget child;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: padding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (title != null) ...[
              Row(
                children: [
                  if (icon != null) ...[Icon(icon, size: 20, color: theme.colorScheme.primary), const SizedBox(width: 8)],
                  Expanded(child: Text(title!, style: theme.textTheme.titleMedium)),
                ],
              ),
              const SizedBox(height: 12),
            ],
            child,
          ],
        ),
      ),
    );
  }
}
