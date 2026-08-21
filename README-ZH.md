文档语言: [English](README.md) | [中文简体](README-ZH.md)

# qrscan

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Pub](https://img.shields.io/pub/v/qrscan.svg)](https://pub.dev/packages/qrscan)

一次性扫码插件：**打开相机，扫到一条，关掉，返回字符串。** 顺带相册 / 路径 / 字节识码，以及生成二维码 PNG。

**不是**页面里嵌的预览控件。要扫码窗、叠加层、手电 API、连续扫，用 [`mobile_scanner`](https://pub.dev/packages/mobile_scanner)——那条赛道已经结束了。

**不是**硬件枪。键盘槳入走 `TextField`。工业 PDA 广播走 [`pda_scanner`](https://github.com/wu9007/pda_scanner)。别为枪开相机。

- **Android**：CameraX 1.4 + ZXing 3.5.3（QR、Code 128/39/93、EAN、UPC、ITF、Data Matrix、PDF417、Aztec、Codabar）
- **iOS**：AVFoundation + Vision
- Dart 3 / Flutter 3.10+ / Android embedding v2
- 公开 Dart API 仍是 0.3.x 那五个函数

## 安装

**pub.dev 还停在 0.3.3。** `0.4.0` 目前只在 git。`qrscan: ^0.4.0` 现在解析不到。

```yaml
dependencies:
  qrscan:
    git:
      url: https://github.com/wu9007/qrcode_scanner.git
      ref: master
```

等发到 pub 之后再写成：

```yaml
dependencies:
  qrscan: ^0.4.0
```

### Android

`minSdk` **21**。相机权限在调用 `scan()` 时申请，不是一安装就抢。不要再加读写存储权限，相册走系统选择器。

宿主 `MainActivity` 必须是 embedding v2（`FlutterActivity`）。

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
String? fromPath = await scanner.scanPath(path);
String? fromBytes = await scanner.scanBytes(bytes);
Uint8List qrPng = await scanner.generateBarCode('https://github.com/wu9007/qrcode_scanner');
```

| 调用 | 返回 |
| --- | --- |
| `scan()` | 解码字符串；用户取消为 `null` |
| `scanPhoto()` | 系统相册选图再解码；取消为 `null` |
| `scanPath(path)` | 本地文件 |
| `scanBytes(bytes)` | 图片字节 |
| `generateBarCode(code)` | 二维码 PNG `Uint8List` |

拒绝相机权限会抛 `PlatformException`，code 为 `PERMISSION_NOT_GRANTED`（常量 `CameraAccessDenied`）。

API 就这些。没有 Widget、没有扫码区域参数、没有前后摄像头切换、没有码流。

## 什么时候不要用

| 你要的是 | 改用 |
| --- | --- |
| 页面内预览 / 扫码窗 / 叠加层 / 连续扫 / Web / 桌面 | [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) |
| 工业 PDA 扳机走 Intent 广播 | [`pda_scanner`](https://github.com/wu9007/pda_scanner) |
| 枪设成键盘槳入（HID） | 任何 `TextField`。不装插件 |

三条路互斥：**相机扫一下**（本插件）· **厂商广播**（`pda_scanner`）· **键盘槳入**（什么都不用）。混用是现场「插件坏了」的第一原因。

## 0.4 复活了什么

| 以前 | 现在 |
| --- | --- |
| 仅 Android，embedding v1 残留 | Android + iOS，embedding v2 |
| JitPack `leyan95/android-zxingLibrary` 经常失踪 | Maven Central `zxing:core:3.5.3` + CameraX 1.4 |
| iOS 是 TODO | Vision 真扫 |
| 乱要存储权限 | 相机 + 系统选择器 |
| SDK `<3.0.0` | Dart 3 / Flutter 3.10+ |
| 没手电的设备点手电崩溃 | CameraX `enableTorch`，没有就 toast |

公开 Dart API 没变。

## License

MIT. Created by Shusheng.
