import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:qrscan/qrscan.dart' as scanner;

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _outputBarcode = '';
  Uint8List bytes = Uint8List(0);
  TextEditingController _inputController;

  @override
  initState() {
    super.initState();
    this._inputController = new TextEditingController();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Qrcode Scanner Example'),
        ),
        backgroundColor: Colors.grey[300],
        body: ListView(
          children: <Widget>[
            _qrCodeWidget(this.bytes),
            Container(
              color: Colors.white,
              child: Column(
                children: <Widget>[
                  TextField(
                    controller: this._inputController,
                    keyboardType: TextInputType.text,
                    textInputAction: TextInputAction.go,
                    onSubmitted: (value) => _generateBarCode(value),
                    decoration: InputDecoration(
                      prefixIcon: Icon(Icons.text_fields),
                      helperText: 'Please input your code to generage qrcode image.',
                      hintText: 'Please Input Your Code',
                      hintStyle: TextStyle(fontSize: 15),
                      contentPadding: EdgeInsets.symmetric(horizontal: 7, vertical: 15),
                    ),
                  ),
                  SizedBox(height: 20),
                  Text('RESULT  $_outputBarcode'),
                  SizedBox(height: 20),
                  Row(
                    children: <Widget>[
                      Expanded(
                        flex: 1,
                        child: SizedBox(
                          height: 100,
                          child: InkWell(
                            onTap: () => _generateBarCode(this._inputController.text),
                            child: Card(child: Text("Generate")),
                          ),
                        ),
                      ),
                      Expanded(
                        flex: 1,
                        child: SizedBox(
                          height: 100,
                          child: InkWell(
                            onTap: _scan,
                            child: Card(child: Text("Scan")),
                          ),
                        ),
                      ),
                      Expanded(
                        flex: 1,
                        child: SizedBox(
                          height: 100,
                          child: InkWell(
                            onTap: _scanPhoto,
                            child: Card(child: Text("Scan Photo")),
                          ),
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 70),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _qrCodeWidget(Uint8List bytes) {
    return Padding(
      padding: EdgeInsets.all(20),
      child: Card(
        elevation: 6,
        child: Column(
          children: <Widget>[
            Container(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: <Widget>[
                  Icon(Icons.verified_user, size: 18, color: Colors.green),
                  Text('  Generate Qrcode', style: TextStyle(fontSize: 15)),
                  Spacer(),
                  Icon(Icons.more_vert, size: 18, color: Colors.black54),
                ],
              ),
              padding: EdgeInsets.symmetric(horizontal: 10, vertical: 9),
              decoration: BoxDecoration(
                color: Colors.black12,
                borderRadius: BorderRadius.only(topLeft: Radius.circular(4), topRight: Radius.circular(4)),
              ),
            ),
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 40, vertical: 15),
              child: Column(
                children: <Widget>[
                  SizedBox(
                    height: 220,
                    child: bytes.isEmpty
                        ? Center(
                            child: Text('Empty code ... ', style: TextStyle(color: Colors.black38)),
                          )
                        : Image.memory(bytes),
                  ),
                  Padding(
                    padding: EdgeInsets.only(top: 7),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: <Widget>[
                        GestureDetector(
                          child: Text('remove', style: TextStyle(fontSize: 15, color: Colors.blue)),
                          onTap: () => this.setState(() => this.bytes = Uint8List(0)),
                        ),
                        Text('    |    ', style: TextStyle(fontSize: 15, color: Colors.black26)),
                        Text('save', style: TextStyle(fontSize: 15, color: Colors.blue)),
                      ],
                    ),
                  )
                ],
              ),
            ),
            Divider(height: 2, color: Colors.black26),
            Container(
              child: Row(
                children: <Widget>[
                  Icon(Icons.history, size: 16, color: Colors.black38),
                  Text('  Generate History', style: TextStyle(fontSize: 14, color: Colors.black38)),
                  Spacer(),
                  Icon(Icons.chevron_right, size: 16, color: Colors.black38),
                ],
              ),
              padding: EdgeInsets.symmetric(horizontal: 10, vertical: 9),
            )
          ],
        ),
      ),
    );
  }

  Future _scan() async {
    String barcode = await scanner.scan();
    setState(() => this._outputBarcode = barcode);
  }

  Future _scanPhoto() async {
    String barcode = await scanner.scanPhoto();
    setState(() => this._outputBarcode = barcode);
  }

  Future _generateBarCode(String inputCode) async {
    Uint8List result = await scanner.generateBarCode(inputCode);
    this.setState(() => this.bytes = result);
  }
}
