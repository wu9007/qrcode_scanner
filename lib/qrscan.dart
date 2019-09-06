import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

/// camera access denied const.
const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';

/// method channel.
const MethodChannel _channel = const MethodChannel('qr_scan');

/// Scanning Bar Code or QR Code return content
Future<String> scan() async => await _channel.invokeMethod('scan');

/// Scanning Photo Bar Code or QR Code return content
Future<String> scanPhoto() async => await _channel.invokeMethod('scan_photo');

// Scanning the image of the specified path
Future<String> scanPath(String path) async {
  assert(path != null && path.isNotEmpty);
  return await _channel.invokeMethod('scan_path', {"path": path});
}

// Parse to code string with uint8list
Future<String> scanBytes(Uint8List uint8list) async {
  assert(uint8list != null && uint8list.isNotEmpty);
  return await _channel.invokeMethod('scan_bytes', {"bytes": uint8list});
}

/// Generating Bar Code Uint8List
Future<Uint8List> generateBarCode(String code) async {
  assert(code != null && code.isNotEmpty);
  return await _channel.invokeMethod('generate_barcode', {"code": code});
}
