import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

void main() {
  testWidgets('Supply list fetches and displays supplies', (tester) async {
    final app = TestApp();
    
    await app.identify();
    
    // We navigate to a screen hosting the SupplyListScreen since it's a tab usually
    await app.pump(tester);
    
    // Switch to Officer Mode
    await tester.tap(find.byIcon(Icons.settings_outlined));
    await tester.pumpAndSettle();
    
    // Toggle Officer Mode switch
    await tester.tap(find.text('Officer mode'));
    await tester.pumpAndSettle();
    
    // Now Relief tab should be visible. In our UI, toggling the switch while on Settings (index 2)
    // might teleport us to Relief (index 2 when officerMode=true). Let's just tap Relief to be sure.
    // If it's already selected, tapping it does nothing.
    await tester.tap(find.text('Relief').last);
    await tester.pumpAndSettle();
    
    // Should display items from DemoDewecsApi
    expect(find.text('Bottled water'), findsOneWidget);
    expect(find.text('Rice (50 kg bags)'), findsOneWidget);
    
    // Type filtering
    await tester.tap(find.widgetWithText(FilterChip, 'Food'));
    await tester.pumpAndSettle();
    
    // Bottled Water should disappear since it's WATER
    expect(find.text('Bottled water'), findsNothing);
    expect(find.text('Rice (50 kg bags)'), findsOneWidget);
  });
}
