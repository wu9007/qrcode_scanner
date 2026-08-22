import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qrscan/qrscan.dart' as scanner;

/// Drop this file into your app. Switch [looks] to [ScanLooks.alipay]
/// or pass your own [ScanLooks(color: …, hint: …)].
class ScanButton extends StatelessWidget {
  const ScanButton({
    super.key,
    this.looks = scanner.ScanLooks.wechat,
    this.label = '扫一扫',
    this.onCode,
  });

  final scanner.ScanLooks looks;
  final String label;
  final ValueChanged<String>? onCode;

  Future<void> _scan(BuildContext context) async {
    try {
      final String? code = await scanner.scan(looks: looks);
      if (code != null) onCode?.call(code);
    } on PlatformException catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.code)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return FilledButton(
      style: FilledButton.styleFrom(
        backgroundColor: looks.color,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(48),
      ),
      onPressed: () => _scan(context),
      child: Text(label),
    );
  }
}
