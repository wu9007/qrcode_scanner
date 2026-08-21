import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:qrscan/qrscan.dart' as scanner;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Uint8List bytes = Uint8List(0);
  final TextEditingController _inputController = TextEditingController();
  final TextEditingController _outputController = TextEditingController();

  @override
  void dispose() {
    _inputController.dispose();
    _outputController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        backgroundColor: Colors.grey[300],
        body: Builder(
          builder: (BuildContext context) {
            return ListView(
              children: <Widget>[
                _qrCodeWidget(bytes, context),
                Container(
                  color: Colors.white,
                  child: Column(
                    children: <Widget>[
                      TextField(
                        controller: _inputController,
                        keyboardType: TextInputType.url,
                        textInputAction: TextInputAction.go,
                        onSubmitted: _generateBarCode,
                        decoration: const InputDecoration(
                          prefixIcon: Icon(Icons.text_fields),
                          helperText: 'Input text to generate a QR image.',
                          hintText: 'Please input your code',
                          contentPadding: EdgeInsets.symmetric(
                            horizontal: 7,
                            vertical: 15,
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      TextField(
                        controller: _outputController,
                        maxLines: 2,
                        decoration: const InputDecoration(
                          prefixIcon: Icon(Icons.wrap_text),
                          helperText: 'Scan result appears here.',
                          hintText: 'Scan result',
                          contentPadding: EdgeInsets.symmetric(
                            horizontal: 7,
                            vertical: 15,
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      _buttonGroup(),
                      const SizedBox(height: 70),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: _scanBytes,
          tooltip: 'Take a Photo',
          child: const Icon(Icons.camera_alt),
        ),
      ),
    );
  }

  Widget _qrCodeWidget(Uint8List bytes, BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Card(
        elevation: 6,
        child: Column(
          children: <Widget>[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
              decoration: const BoxDecoration(
                color: Colors.black12,
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(4),
                  topRight: Radius.circular(4),
                ),
              ),
              child: const Row(
                children: <Widget>[
                  Icon(Icons.verified_user, size: 18, color: Colors.green),
                  Text('  Generate Qrcode', style: TextStyle(fontSize: 15)),
                  Spacer(),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(
                left: 40,
                right: 40,
                top: 30,
                bottom: 10,
              ),
              child: SizedBox(
                height: 190,
                child: bytes.isEmpty
                    ? const Center(
                        child: Text(
                          'Empty code ... ',
                          style: TextStyle(color: Colors.black38),
                        ),
                      )
                    : Image.memory(bytes),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buttonGroup() {
    return Row(
      children: <Widget>[
        _actionCard('Generate', 'images/generate_qrcode.png',
            () => _generateBarCode(_inputController.text)),
        _actionCard('Scan', 'images/scanner.png', _scan),
        _actionCard('Scan Photo', 'images/albums.png', _scanPhoto),
      ],
    );
  }

  Widget _actionCard(String label, String asset, VoidCallback onTap) {
    return Expanded(
      child: SizedBox(
        height: 120,
        child: InkWell(
          onTap: onTap,
          child: Card(
            child: Column(
              children: <Widget>[
                Expanded(flex: 2, child: Image.asset(asset)),
                const Divider(height: 20),
                Expanded(flex: 1, child: Text(label)),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _scan() async {
    try {
      final String? barcode = await scanner.scan();
      if (barcode != null) {
        _outputController.text = barcode;
      }
    } on PlatformException catch (e) {
      _outputController.text = '${e.code}: ${e.message ?? ''}';
    }
  }

  Future<void> _scanPhoto() async {
    final String? barcode = await scanner.scanPhoto();
    if (barcode != null) {
      _outputController.text = barcode;
    }
  }

  Future<void> _scanBytes() async {
    final XFile? picked =
        await ImagePicker().pickImage(source: ImageSource.camera);
    if (picked == null) {
      return;
    }
    final Uint8List data = await picked.readAsBytes();
    final String? barcode = await scanner.scanBytes(data);
    if (barcode != null) {
      _outputController.text = barcode;
    }
  }

  Future<void> _generateBarCode(String inputCode) async {
    if (inputCode.isEmpty) {
      return;
    }
    final Uint8List result = await scanner.generateBarCode(inputCode);
    setState(() => bytes = result);
  }
}
