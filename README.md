Language: [English](README.md) | [中文简体](README-ZH.md)

# QR Code Scanner
  
[![License][license-image]][license-url] 
[![Pub](https://img.shields.io/pub/v/qrscan.svg?style=flat-square)](https://pub.dartlang.org/packages/qrscan)

A Flutter plugin 🛠 to scanning. Ready for Android 🚀
base on ZXing [github](https://github.com/yipianfengye/android-zxingLibrary)

#### permission：
- `<uses-permission android:name="android.permission.INTERNET"/>`
- `<uses-permission android:name="android.permission.CAMERA" />`
- `<uses-permission android:name="android.permission.VIBRATE"/>`

## Installation

Add this to your package's pubspec.yaml file:

```
dependencies:
 qrscan: ^0.1.0
```

## Usage example
```dart
String barcode = await Qrscan.scan();
```

## Contribute

We would ❤️ to see your contribution!

## License

Distributed under the MIT license. See ``LICENSE`` for more information.

## About

Created by Shusheng.

[license-image]: https://img.shields.io/badge/License-MIT-blue.svg
[license-url]: LICENSE
