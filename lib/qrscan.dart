/// One-shot Flutter QR / barcode plugin.
///
/// Open the camera, decode the first code, return a [String], close.
/// Not an embedded preview widget — use `mobile_scanner` for that.
library qrscan;

import 'dart:async';
import 'dart:ui' show Color;

import 'package:flutter/services.dart';

/// Returned when the host denies camera access.
// ignore: constant_identifier_names
const String CameraAccessDenied = 'PERMISSION_NOT_GRANTED';

/// Camera bind failed (no back camera, driver error).
const String CameraStartFailed = 'CAMERA_START_FAILED';

/// Another app already holds the camera.
const String CameraInUse = 'CAMERA_IN_USE';

/// Plugin has no Activity / cannot present the scanner.
const String NoActivity = 'NO_ACTIVITY';

const MethodChannel _channel = MethodChannel('qr_scan');

/// Packaged [color] + [hint]. Still the same two overlay knobs.
class ScanLooks {
  const ScanLooks({required this.color, this.hint});

  final Color color;
  final String? hint;

  /// WeChat green. 「将二维码/条码放入框内，即可自动扫描」
  static const ScanLooks wechat = ScanLooks(
    color: Color(0xFF07C160),
    hint: '将二维码/条码放入框内，即可自动扫描',
  );

  /// Alipay blue. 「对准二维码/条码」
  static const ScanLooks alipay = ScanLooks(
    color: Color(0xFF1677FF),
    hint: '对准二维码/条码',
  );
}

/// Open the camera scanner and return the decoded string, or `null` if cancelled.
///
/// This does not open the gallery. Use [scanPhoto] for that.
///
/// [color] tints the corner marks and the scan line (default `#12C4FF`).
/// [hint] is a line under the viewfinder.
/// [looks] is just [ScanLooks.wechat] / [ScanLooks.alipay] — same two knobs.
/// Explicit [color] / [hint] win over [looks]. Omit all three and `scan()` is
/// the 2019 call.
Future<String?> scan({Color? color, String? hint, ScanLooks? looks}) async {
  final Color? tint = color ?? looks?.color;
  final String? copy = hint ?? looks?.hint;
  final Map<String, Object> args = <String, Object>{};
  if (tint != null) {
    // Color.value is ARGB. toARGB32() needs a newer Flutter than 3.10.
    // ignore: deprecated_member_use
    args['color'] = tint.value;
  }
  if (copy != null) {
    args['hint'] = copy;
  }
  return await _channel.invokeMethod<String>('scan', args);
}

/// Pick an image from the gallery and decode a barcode / QR code.
Future<String?> scanPhoto() async =>
    await _channel.invokeMethod<String>('scan_photo');

/// Decode a barcode / QR code from a local file path.
Future<String?> scanPath(String path) async {
  if (path.isEmpty) {
    throw ArgumentError.value(path, 'path', 'must not be empty');
  }
  return await _channel.invokeMethod<String>('scan_path', {'path': path});
}

/// Decode a barcode / QR code from image bytes.
Future<String?> scanBytes(Uint8List uint8list) async {
  if (uint8list.isEmpty) {
    throw ArgumentError.value(uint8list, 'uint8list', 'must not be empty');
  }
  return await _channel.invokeMethod<String>('scan_bytes', {'bytes': uint8list});
}

/// Generate a QR code PNG as bytes. Name is historical — this is QR only.
Future<Uint8List> generateBarCode(String code) async {
  if (code.isEmpty) {
    throw ArgumentError.value(code, 'code', 'must not be empty');
  }
  final Uint8List? data =
      await _channel.invokeMethod<Uint8List>('generate_barcode', {'code': code});
  if (data == null) {
    throw StateError('Failed to generate QR code');
  }
  return data;
}
