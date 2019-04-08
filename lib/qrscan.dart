import 'dart:async';

import 'package:flutter/services.dart';

/*
    Author: Shusheng
    Email: leyansorosame@gmail.com
    createTime:2019-04-07 21:39
 */
class Qrscan {
  // camera access denied const.
  static const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';

  // method channel.
  static const MethodChannel _channel = const MethodChannel('qr_scan');

  // method invoke.
  static Future<String> scan() async => await _channel.invokeMethod('scan');
}
