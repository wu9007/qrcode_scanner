import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qrscan/qrscan.dart' as scanner;

void main() {
  runApp(const DemoApp());
}

class DemoApp extends StatelessWidget {
  const DemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF12C4FF),
          brightness: Brightness.light,
        ),
      ),
      home: const DemoPage(),
    );
  }
}

class DemoPage extends StatefulWidget {
  const DemoPage({super.key});

  @override
  State<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends State<DemoPage> {
  String? _result;
  Uint8List? _png;
  scanner.ScanLooks _looks = scanner.ScanLooks.wechat;

  Future<void> _scan() async {
    try {
      final String? code = await scanner.scan(looks: _looks);
      if (!mounted) return;
      setState(() => _result = code);
    } on PlatformException catch (e) {
      if (!mounted) return;
      setState(() => _result = e.code);
    }
  }

  Future<void> _photo() async {
    final String? code = await scanner.scanPhoto();
    if (!mounted || code == null) return;
    setState(() => _result = code);
  }

  Future<void> _make() async {
    final Uint8List png = await scanner.generateBarCode(
      _result ?? 'https://github.com/wu9007/qrcode_scanner',
    );
    if (!mounted) return;
    setState(() => _png = png);
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme cs = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: cs.surface,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 28, 24, 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Text(
                'qrscan',
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      letterSpacing: 1.4,
                      color: cs.primary,
                    ),
              ),
              const SizedBox(height: 28),
              Expanded(
                child: Center(
                  child: _png != null
                      ? Image.memory(_png!, width: 180, height: 180)
                      : Text(
                          _result ?? '点扫描',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.headlineSmall
                              ?.copyWith(
                                color: _result == null
                                    ? cs.onSurface.withValues(alpha: 0.28)
                                    : cs.onSurface,
                                height: 1.35,
                              ),
                        ),
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                children: <Widget>[
                  _LooksChip(
                    label: '微信',
                    color: scanner.ScanLooks.wechat.color,
                    selected: _looks == scanner.ScanLooks.wechat,
                    onTap: () => setState(() => _looks = scanner.ScanLooks.wechat),
                  ),
                  _LooksChip(
                    label: '支付宝',
                    color: scanner.ScanLooks.alipay.color,
                    selected: _looks == scanner.ScanLooks.alipay,
                    onTap: () => setState(() => _looks = scanner.ScanLooks.alipay),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              FilledButton(
                onPressed: _scan,
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                ),
                child: const Text('扫描'),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  TextButton(onPressed: _photo, child: const Text('相册')),
                  TextButton(onPressed: _make, child: const Text('生码')),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LooksChip extends StatelessWidget {
  const _LooksChip({
    required this.label,
    required this.color,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final Color color;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return FilterChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => onTap(),
      selectedColor: color.withValues(alpha: 0.22),
      checkmarkColor: color,
      side: BorderSide(color: color),
    );
  }
}
