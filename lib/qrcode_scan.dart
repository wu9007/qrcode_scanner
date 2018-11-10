import 'dart:async';

import 'package:flutter/services.dart';

class QrcodeScan {
  static const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';
  static const MethodChannel _channel =
  const MethodChannel('com.leyan.qrcode_scan');
  static Future<String> scan() async => await _channel.invokeMethod('scan');
}
