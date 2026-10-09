import 'package:flutter/material.dart';

import '../screens/identify_screen.dart';
import '../strings.dart';

/// Shown instead of Home and New report until the citizen has identified.
class NeedsIdentity extends StatelessWidget {
  const NeedsIdentity({super.key});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 44,
              backgroundColor: Theme.of(context).colorScheme.primaryContainer,
              child: Icon(Icons.badge_outlined, size: 44, color: Theme.of(context).colorScheme.onPrimaryContainer),
            ),
            const SizedBox(height: 16),
            Text(S.needsIdentityTitle, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 8),
            Text(S.needsIdentityBody, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute<bool>(builder: (_) => const IdentifyScreen()),
              ),
              child: const Text(S.identifyButton),
            ),
          ],
        ),
      ),
    );
  }
}
