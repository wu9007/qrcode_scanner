Language: [English](README.md) | [中文简体](README-ZH.md)

# qrscan

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Pub](https://img.shields.io/pub/v/qrscan.svg)](https://pub.dev/packages/qrscan)

Flutter plugin for scanning QR codes and barcodes, and generating QR images.

- **Android** CameraX + ZXing (QR, Code 128/39/93, EAN, UPC, ITF, Data Matrix, PDF417, Aztec, Codabar)
- **iOS** AVFoundation + Vision
- Dart 3 / Flutter 3.10+ / Android embedding v2

## Install

```yaml
dependencies:
  qrscan: ^0.4.0
```

### Android

`minSdk` **21**. Camera permission is requested at runtime. Do **not** add storage permissions — album picking uses the system picker.

### iOS

Add to `ios/Runner/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Scan QR codes and barcodes</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Read a code from a photo</string>
```

Minimum iOS 12.

## Usage

```dart
import 'package:qrscan/qrscan.dart' as scanner;

String? cameraScanResult = await scanner.scan();
String? photoScanResult = await scanner.scanPhoto();
String? fromPath = await scanner.scanPath(path);
String? fromBytes = await scanner.scanBytes(bytes);
Uint8List qrPng = await scanner.generateBarCode('https://github.com/wu9007/qrcode_scanner');
```

`scan()` returns `null` when the user cancels. Camera denial throws a `PlatformException` with code `PERMISSION_NOT_GRANTED`.

## 0.3 → 0.4

| Before | After |
| --- | --- |
| Android only, embedding v1 leftovers | Android + iOS, embedding v2 |
| `com.github.leyan95:android-zxingLibrary` (JitPack, often missing) | `com.google.zxing:core` + CameraX |
| Storage permissions | Camera + system photo picker |
| SDK `<3.0.0` | Dart 3 |

Public Dart API is unchanged.

## License

MIT. Created by Shusheng.
