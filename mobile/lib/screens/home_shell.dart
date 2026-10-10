import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/theme.dart';
import '../state/identity_controller.dart';
import '../state/rescue_requests_controller.dart';
import '../state/settings_controller.dart';
import '../state/shelters_controller.dart';
import '../strings.dart';
import '../widgets/needs_identity.dart';
import 'home_screen.dart';
import 'identify_screen.dart';
import 'new_report_screen.dart';
import 'rescue_list_screen.dart';
import 'settings_screen.dart';
import 'shelter_list_screen.dart';
import 'supply_list_screen.dart';

/// Bottom navigation between My reports, New report, Shelters, Rescue and Settings. My reports and New report need an
/// identified citizen; the shelter and rescue pages of the officers do not.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  static const _sheltersTab = 2;
  static const _rescueTab = 3;

  /// The shelter and rescue lists load from the server when they are built, so they are built when first opened.
  final Set<int> _opened = {0};
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

  void _select(int i) {
    final seenBefore = _opened.contains(i);
    setState(() {
      _index = i;
      _opened.add(i);
    });
    // A list that was already built is fetched again when its tab is selected, so it never shows stale or cleared
    // data (for example after Demo mode was switched). The first visit loads itself.
    if (seenBefore && i == _sheltersTab) {
      context.read<SheltersController>().refresh();
    } else if (seenBefore && i == _rescueTab) {
      context.read<RescueRequestsController>().refresh();
    }
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
      S.navShelters,
      S.navRescue,
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
        _opened.contains(_sheltersTab) ? const ShelterListScreen() : const SizedBox.shrink(),
        _opened.contains(_rescueTab) ? const RescueListScreen() : const SizedBox.shrink(),
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
        onDestinationSelected: _select,
        destinations: [
          const NavigationDestination(icon: Icon(Icons.list_alt_outlined), selectedIcon: Icon(Icons.list_alt), label: S.navHome),
          const NavigationDestination(icon: Icon(Icons.add_circle_outline), selectedIcon: Icon(Icons.add_circle), label: S.navNewReport),
          const NavigationDestination(icon: Icon(Icons.night_shelter_outlined), selectedIcon: Icon(Icons.night_shelter), label: S.navShelters),
          const NavigationDestination(icon: Icon(Icons.support_outlined), selectedIcon: Icon(Icons.support), label: S.navRescue),
          if (officerMode) const NavigationDestination(icon: Icon(Icons.inventory_2_outlined), selectedIcon: Icon(Icons.inventory_2), label: S.navRelief),
          const NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: S.navSettings),
        ],
      ),
    );
  }
}
