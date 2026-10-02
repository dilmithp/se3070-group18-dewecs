import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/theme.dart';
import '../state/identity_controller.dart';
import '../state/settings_controller.dart';
import '../strings.dart';
import '../widgets/needs_identity.dart';
import 'home_screen.dart';
import 'identify_screen.dart';
import 'new_report_screen.dart';
import 'settings_screen.dart';
import 'supply_list_screen.dart';

/// Bottom navigation between My reports, New report and Settings. Home and New report need an identified citizen.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  bool _wasIdentified = false;

  @override
  void initState() {
    super.initState();
    _wasIdentified = context.read<IdentityController>().isIdentified;
    // First run: go straight to the identify form.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && !context.read<IdentityController>().isIdentified) {
        _openIdentify();
      }
    });
  }

  void _openIdentify() {
    Navigator.of(context).push(MaterialPageRoute<bool>(builder: (_) => const IdentifyScreen()));
  }

  /// The identity disappeared while the app was running (the server was reset, or Demo mode was switched):
  /// explain and go to the identify form. Reports waiting on the phone are kept.
  void _identityChanged(bool identified) {
    if (_wasIdentified && !identified) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) {
          return;
        }
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text(S.identityReset)));
        _openIdentify();
      });
    }
    _wasIdentified = identified;
  }

  @override
  Widget build(BuildContext context) {
    final identified = context.watch<IdentityController>().isIdentified;
    _identityChanged(identified);
    final officerMode = context.watch<SettingsController>().officerMode;

    // IndexedStack keeps the half-filled report form alive while the user looks at another tab.
    // Index logic needs care since the Relief tab can appear/disappear.
    final titles = <String>[
      S.navHome,
      S.navNewReport,
      if (officerMode) S.navRelief,
      S.navSettings,
    ];

    var safeIndex = _index;
    if (safeIndex >= titles.length) {
      safeIndex = titles.length - 1;
    }
    
    final Widget body = IndexedStack(
      index: safeIndex,
      children: [
        identified ? HomeScreen(onNewReport: () => setState(() => _index = 1)) : const NeedsIdentity(),
        identified ? NewReportScreen(onDone: () => setState(() => _index = 0)) : const NeedsIdentity(),
        if (officerMode) const SupplyListScreen(),
        const SettingsScreen(),
      ],
    );

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            const Icon(Icons.shield, size: 22, color: AppColors.focusOnDark),
            const SizedBox(width: 10),
            Flexible(child: Text(titles[safeIndex], overflow: TextOverflow.ellipsis)),
          ],
        ),
      ),
      body: body,
      bottomNavigationBar: NavigationBar(
        selectedIndex: safeIndex,
        onDestinationSelected: (i) {
          setState(() => _index = i);
        },
        destinations: [
          const NavigationDestination(icon: Icon(Icons.list_alt_outlined), selectedIcon: Icon(Icons.list_alt), label: S.navHome),
          const NavigationDestination(icon: Icon(Icons.add_circle_outline), selectedIcon: Icon(Icons.add_circle), label: S.navNewReport),
          if (officerMode) const NavigationDestination(icon: Icon(Icons.inventory_2_outlined), selectedIcon: Icon(Icons.inventory_2), label: S.navRelief),
          const NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: S.navSettings),
        ],
      ),
    );
  }
}
