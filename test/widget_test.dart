import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:waypoint/main.dart';

void main() {
  testWidgets('App renders without crashing', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: WaypointApp()));
    await tester.pump();
    expect(find.byType(WaypointApp), findsOneWidget);
  });
}
