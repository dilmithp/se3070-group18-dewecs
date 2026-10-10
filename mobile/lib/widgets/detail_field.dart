import 'package:flutter/material.dart';

/// A small grey label over a value: one row of a detail card, the same as a web `<dt>` and `<dd>` pair.
class DetailField extends StatelessWidget {
  const DetailField({super.key, required this.label, this.value, this.child, this.first = false})
    : assert(value != null || child != null, 'give a value or a child');

  final String label;
  final String? value;

  /// Shown instead of [value] when the value is more than text (a badge, a progress bar).
  final Widget? child;
  final bool first;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: EdgeInsets.only(top: first ? 0 : 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: theme.textTheme.labelLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          const SizedBox(height: 2),
          child ?? Text(value!, style: theme.textTheme.bodyLarge),
        ],
      ),
    );
  }
}
