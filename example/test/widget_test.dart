import 'package:flutter_test/flutter_test.dart';
import 'package:qrscan_example/main.dart';

void main() {
  testWidgets('one Scan button', (WidgetTester tester) async {
    await tester.pumpWidget(const DemoApp());
    expect(find.text('扫描'), findsOneWidget);
    expect(find.text('点扫描'), findsOneWidget);
  });
}
