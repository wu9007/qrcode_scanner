文档语言: [English](https://github.com/flutterchina/qrscan) | [中文简体](README-ZH.md)

# 二维码扫描插件
  
[![License][license-image]][license-url] 
[![Pub](https://img.shields.io/pub/v/qrscan.svg?style=flat-square)](https://pub.dartlang.org/packages/qrscan)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2564729935f441b4987fd4f49ac988d8)](https://www.codacy.com/app/leyan95/qrcode_scanner?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=leyan95/qrcode_scanner&amp;utm_campaign=Badge_Grade)

A Flutter plugin 🛠 to scanning. Ready for Android 🚀
base on ZXing [github](https://github.com/leyan95/qrcode_scanner)

## 权限：
`<uses-permission android:name="android.permission.CAMERA" />`
`<uses-permission android:name="android.permission.VIBRATE"/>`

## 安装

Add this to your package's pubspec.yaml file:

```yaml
dependencies:
 qrscan: ^0.2.18
```

## 使用方式
```dart
import 'package:qrscan/qrscan.dart' as scanner;

String barcode = await scanner.scan();

String photoScanResult = await scanner.scanPhoto();
```

## 贡献

We would ❤️ to see your contribution!

## 许可

Distributed under the MIT license. See ``LICENSE`` for more information.

## 关于

Created by Shusheng.

[license-image]: https://img.shields.io/badge/License-MIT-blue.svg
[license-url]: LICENSE
