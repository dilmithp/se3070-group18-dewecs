import 'package:dewecs_mobile/screens/identify_screen.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

void main() {
  testWidgets('not identified: Home and New report ask the user to identify, Settings stays reachable', (tester) async {
    await TestApp().pump(tester);
    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(find.text(S.needsIdentityTitle), findsOneWidget);

    await tester.tap(find.text(S.navNewReport));
    await tester.pumpAndSettle();
    expect(find.text(S.needsIdentityTitle), findsOneWidget);

    await tester.tap(find.text(S.navSettings));
    await tester.pumpAndSettle();
    expect(find.text(S.settingsServer), findsOneWidget);
    expect(find.text(S.needsIdentityTitle), findsNothing);
  });

  testWidgets('the identify button on the prompt opens the identify form', (tester) async {
    await TestApp().pump(tester);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.text(S.identifyButton));
    await tester.pumpAndSettle();

    expect(find.byType(IdentifyScreen), findsOneWidget);
  });

  testWidgets('identified: no identify form on start', (tester) async {
    final app = TestApp();
    await app.dependencies.identity
        .identify(nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1);

    await app.pump(tester);

    expect(find.byType(IdentifyScreen), findsNothing);
    expect(find.text(S.needsIdentityTitle), findsNothing);
  });
}
