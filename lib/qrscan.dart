import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

/// Returned when the host denies camera access.
// ignore: constant_identifier_names
const String CameraAccessDenied = 'PERMISSION_NOT_GRANTED';

const MethodChannel _channel = MethodChannel('qr_scan');

/// Open the camera scanner and return the decoded string, or `null` if cancelled.
Future<String?> scan() async => await _channel.invokeMethod<String>('scan');

/// Pick an image from the gallery and decode a barcode / QR code.
Future<String?> scanPhoto() async =>
    await _channel.invokeMethod<String>('scan_photo');

/// Decode a barcode / QR code from a local file path.
Future<String?> scanPath(String path) async {
  assert(path.isNotEmpty);
  return await _channel.invokeMethod<String>('scan_path', {'path': path});
}

/// Decode a barcode / QR code from image bytes.
Future<String?> scanBytes(Uint8List uint8list) async {
  assert(uint8list.isNotEmpty);
  return await _channel.invokeMethod<String>('scan_bytes', {'bytes': uint8list});
}

/// Generate a QR code PNG as bytes.
Future<Uint8List> generateBarCode(String code) async {
  assert(code.isNotEmpty);
  final Uint8List? data =
      await _channel.invokeMethod<Uint8List>('generate_barcode', {'code': code});
  if (data == null) {
    throw StateError('Failed to generate QR code');
  }
  return data;
}
