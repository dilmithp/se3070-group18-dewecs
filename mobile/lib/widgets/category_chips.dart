import 'package:flutter/material.dart';

import '../strings.dart';

IconData categoryIcon(String category) {
  switch (category) {
    case 'FLOOD':
      return Icons.water;
    case 'LANDSLIDE':
      return Icons.landscape;
    case 'CYCLONE':
      return Icons.cyclone;
    case 'DROUGHT':
      return Icons.wb_sunny;
  }
  return Icons.warning_amber;
}

/// Choice chips with an icon and a text label per hazard category (unknown categories still get a chip).
class CategoryChips extends StatelessWidget {
  const CategoryChips({super.key, required this.categories, required this.selected, required this.onSelected});

  final List<String> categories;
  final String? selected;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final category in categories)
          ChoiceChip(
            avatar: Icon(categoryIcon(category), size: 20),
            label: Text(S.categoryName(category)),
            selected: selected == category,
            onSelected: (_) => onSelected(category),
          ),
      ],
    );
  }
}
