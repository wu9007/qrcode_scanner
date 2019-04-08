文档语言: [English](https://github.com/flutterchina/qrscan) | [中文简体](README-ZH.md)

# 二维码扫描插件
  
[![License][license-image]][license-url] 
[![Pub](https://img.shields.io/pub/v/qrscan.svg?style=flat-square)](https://pub.dartlang.org/packages/qrscan)

A Flutter plugin 🛠 to scanning. Ready for Android 🚀
base on ZXing [github](https://github.com/yipianfengye/android-zxingLibrary)

#### 权限：
- `<uses-permission android:name="android.permission.INTERNET"/>`
- `<uses-permission android:name="android.permission.CAMERA" />`
- `<uses-permission android:name="android.permission.VIBRATE"/>`

## 安装

Add this to your package's pubspec.yaml file:

```
dependencies:
 qrscan: ^0.1.0
```

## 使用方式
```dart
import 'package:qrscan/qrscan.dart' as scanner;

String barcode = await scanner.scan();
```

## 贡献

We would ❤️ to see your contribution!

## 许可

Distributed under the MIT license. See ``LICENSE`` for more information.

## 关于

Created by Shusheng.

[license-image]: https://img.shields.io/badge/License-MIT-blue.svg
[license-url]: LICENSE
