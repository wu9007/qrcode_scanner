Language: [English](README.md) | [中文简体](README-ZH.md)

# QR Code Scanner
  
[![License][license-image]][license-url] 
[![Pub](https://img.shields.io/pub/v/qrscan.svg?style=flat-square)](https://pub.dartlang.org/packages/qrscan)

A Flutter plugin 🛠 to scanning. Ready for Android 🚀

[github](https://github.com/leyan95/qrcode_scanner)

![qrscan.gif](./example/android/app/src/main/res/drawable/qr_scan.gif)

## Permission：
`<uses-permission android:name="android.permission.CAMERA" />`

## Installation

Add this to your package's pubspec.yaml file:

```yaml
dependencies:
 qrscan: ^0.2.7
```

## Usage example
```dart
import 'package:qrscan/qrscan.dart' as scanner;

String cameraScanResult = await scanner.scan();
String photoScanResult = await scanner.scanPhoto();
```

## Supported

-  [x] Scan BR-CODE
-  [x] Scan QR-CODE
-  [x] Control the flash while scanning
-  [x] Apply for camera privileges
-  [x] Scanning BR-CODE or QR-CODE in albums
-  [x] Display the switch button of the flashlight according to the light intensity

## Features

-  Generate QR-CODE and BR-CODE
-  Support IOS

## Contribute

We would ❤️ to see your contribution!

## License

Distributed under the MIT license. See ``LICENSE`` for more information.

## About

Created by Shusheng.

[license-image]: https://img.shields.io/badge/License-MIT-blue.svg
[license-url]: LICENSE
