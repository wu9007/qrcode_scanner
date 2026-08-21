文档语言: [English](README.md) | [中文简体](README-ZH.md)

# qrscan 二维码扫描插件

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Pub](https://img.shields.io/pub/v/qrscan.svg)](https://pub.dev/packages/qrscan)

Flutter 扫码 / 生码插件。

- **Android**：CameraX + ZXing
- **iOS**：AVFoundation + Vision
- Dart 3 / Flutter 3.10+ / Android embedding v2

## 安装

```yaml
dependencies:
  qrscan: ^0.4.0
```

### Android

`minSdk` **21**。相机权限运行时申请。不要再加读写存储权限，相册走系统选择器。

### iOS

在 `ios/Runner/Info.plist` 增加：

```xml
<key>NSCameraUsageDescription</key>
<string>用于扫描二维码和条码</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>用于从相册识别二维码</string>
```

最低 iOS 12。

## 用法

```dart
import 'package:qrscan/qrscan.dart' as scanner;

String? cameraScanResult = await scanner.scan();
String? photoScanResult = await scanner.scanPhoto();
Uint8List qrPng = await scanner.generateBarCode('https://github.com/wu9007/qrcode_scanner');
```

用户取消返回 `null`。拒绝相机权限会抛 `PERMISSION_NOT_GRANTED`。

## 0.4 复活了什么

去掉已失效的 JitPack `android-zxingLibrary`、Android embedding v1、Dart 2 SDK 上限；补上 iOS 真扫码；相册不再要存储权限。

## License

MIT. Created by Shusheng.
