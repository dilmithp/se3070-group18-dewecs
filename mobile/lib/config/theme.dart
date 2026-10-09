import 'package:flutter/material.dart';

/// The look of the officer web pages (backend `style.css` design tokens), so the phone app and the web dashboard read
/// as one product: navy top bar, #1D4F91 actions, white bordered cards on a pale grey page, pill badges.
/// Colours come from the colour scheme and [AppColors]; text always carries the meaning, colour only reinforces it.
class AppTheme {
  AppTheme._();

  static ThemeData light() => _build(Brightness.light);

  static ThemeData dark() => _build(Brightness.dark);

  static ThemeData _build(Brightness brightness) {
    final dark = brightness == Brightness.dark;
    final scheme = dark ? _darkScheme : _lightScheme;
    final border = BorderSide(color: scheme.outlineVariant);
    final radius = BorderRadius.circular(8);

    OutlineInputBorder inputBorder(Color color, [double width = 1]) =>
        OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: color, width: width));

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: dark ? const Color(0xFF0F1722) : const Color(0xFFF3F5F8),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.navy,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w700, letterSpacing: 0.3),
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      cardTheme: CardThemeData(
        color: scheme.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: radius, side: border),
      ),
      dividerTheme: DividerThemeData(color: scheme.outlineVariant, space: 1),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surface,
        border: inputBorder(scheme.outline),
        enabledBorder: inputBorder(scheme.outline),
        focusedBorder: inputBorder(scheme.primary, 2),
        errorBorder: inputBorder(scheme.error),
        focusedErrorBorder: inputBorder(scheme.error, 2),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(48, 48),
          shape: RoundedRectangleBorder(borderRadius: radius),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(48, 48),
          foregroundColor: scheme.onSurface,
          side: BorderSide(color: scheme.outline),
          shape: RoundedRectangleBorder(borderRadius: radius),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(minimumSize: const Size(48, 48), shape: RoundedRectangleBorder(borderRadius: radius)),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: scheme.surface,
        selectedColor: scheme.primaryContainer,
        side: BorderSide(color: scheme.outline),
        labelStyle: TextStyle(color: scheme.onSurface, fontWeight: FontWeight.w600),
        secondaryLabelStyle: TextStyle(color: scheme.onPrimaryContainer, fontWeight: FontWeight.w700),
        checkmarkColor: scheme.onPrimaryContainer,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surface,
        indicatorColor: scheme.primaryContainer,
        surfaceTintColor: Colors.transparent,
        height: 68,
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            fontSize: 12.5,
            fontWeight: states.contains(WidgetState.selected) ? FontWeight.w700 : FontWeight.w500,
            color: states.contains(WidgetState.selected) ? scheme.onPrimaryContainer : scheme.onSurfaceVariant,
          ),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: AppColors.navy,
        contentTextStyle: const TextStyle(color: Colors.white),
        shape: RoundedRectangleBorder(borderRadius: radius),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: radius),
      ),
      listTileTheme: const ListTileThemeData(contentPadding: EdgeInsets.zero),
      textTheme: const TextTheme(
        headlineSmall: TextStyle(fontWeight: FontWeight.w700),
        titleLarge: TextStyle(fontWeight: FontWeight.w700),
        titleMedium: TextStyle(fontWeight: FontWeight.w700),
        titleSmall: TextStyle(fontWeight: FontWeight.w700),
      ),
    );
  }

  static const _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF1D4F91),
    onPrimary: Colors.white,
    primaryContainer: Color(0xFFE1EDFB),
    onPrimaryContainer: Color(0xFF0F3D75),
    secondary: Color(0xFF455063),
    onSecondary: Colors.white,
    secondaryContainer: Color(0xFFFFEFC2),
    onSecondaryContainer: Color(0xFF664500),
    tertiary: Color(0xFF664500),
    onTertiary: Colors.white,
    tertiaryContainer: Color(0xFFFFEFC2),
    onTertiaryContainer: Color(0xFF664500),
    error: Color(0xFFB42318),
    onError: Colors.white,
    errorContainer: Color(0xFFFDECEA),
    onErrorContainer: Color(0xFF7A1B13),
    surface: Colors.white,
    onSurface: Color(0xFF1B2430),
    onSurfaceVariant: Color(0xFF455063),
    surfaceContainerHighest: Color(0xFFE6EAF0),
    outline: Color(0xFF768192),
    outlineVariant: Color(0xFFD5DBE3),
    shadow: Color(0xFF12263F),
  );

  static const _darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFF8AB4F8),
    onPrimary: Color(0xFF0B2545),
    primaryContainer: Color(0xFF1D3A63),
    onPrimaryContainer: Color(0xFFD6E6FB),
    secondary: Color(0xFFB8C2D1),
    onSecondary: Color(0xFF1B2430),
    secondaryContainer: Color(0xFF4A3A00),
    onSecondaryContainer: Color(0xFFFFE9A8),
    tertiary: Color(0xFFFFE9A8),
    onTertiary: Color(0xFF3B2C00),
    tertiaryContainer: Color(0xFF4A3A00),
    onTertiaryContainer: Color(0xFFFFE9A8),
    error: Color(0xFFFF8A80),
    onError: Color(0xFF3B0D0A),
    errorContainer: Color(0xFF5C1A14),
    onErrorContainer: Color(0xFFFFD9D5),
    surface: Color(0xFF1B2430),
    onSurface: Color(0xFFE8EDF4),
    onSurfaceVariant: Color(0xFFB8C2D1),
    surfaceContainerHighest: Color(0xFF2A3545),
    outline: Color(0xFF8A96A8),
    outlineVariant: Color(0xFF3A4658),
    shadow: Colors.black,
  );
}

/// Fixed brand colours shared with the web UI.
class AppColors {
  AppColors._();

  static const navy = Color(0xFF12263F);
  static const navyActive = Color(0xFF284A73);
  static const focusOnDark = Color(0xFFFFD166);

  // Accent colours of the dashboard tiles on the web: warning, shelter, rescue, supply, report.
  static const warning = Color(0xFFB54708);
  static const shelter = Color(0xFF1D4F91);
  static const rescue = Color(0xFFB42318);
  static const supply = Color(0xFF8A6100);
  static const report = Color(0xFF5B3FA0);
}

/// Background and text colour of a pill badge, from the same palette as the web `.badge-*` rules.
class BadgeColors {
  const BadgeColors(this.background, this.foreground);

  final Color background;
  final Color foreground;

  static BadgeColors good(Brightness b) => b == Brightness.dark
      ? const BadgeColors(Color(0xFF12391F), Color(0xFFBFE8CC))
      : const BadgeColors(Color(0xFFE3F4E8), Color(0xFF14532D));

  static BadgeColors info(Brightness b) => b == Brightness.dark
      ? const BadgeColors(Color(0xFF173B66), Color(0xFFCFE2FB))
      : const BadgeColors(Color(0xFFE1EDFB), Color(0xFF0F3D75));

  static BadgeColors attention(Brightness b) => b == Brightness.dark
      ? const BadgeColors(Color(0xFF4A3A00), Color(0xFFFFE9A8))
      : const BadgeColors(Color(0xFFFFEFC2), Color(0xFF664500));

  static BadgeColors bad(Brightness b) => b == Brightness.dark
      ? const BadgeColors(Color(0xFF5A1A1F), Color(0xFFFFD2D2))
      : const BadgeColors(Color(0xFFFAD4D4), Color(0xFF85101A));

  static BadgeColors quiet(Brightness b) => b == Brightness.dark
      ? const BadgeColors(Color(0xFF2E3848), Color(0xFFD5DBE3))
      : const BadgeColors(Color(0xFFE6EAF0), Color(0xFF333D4D));
}
