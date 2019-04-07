import 'dart:async';

import 'package:flutter/services.dart';

class Qrscan {
  static const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';
  static const MethodChannel _channel =
      const MethodChannel('qr_scan');
  static Future<String> scan() async => await _channel.invokeMethod('scan');
}
