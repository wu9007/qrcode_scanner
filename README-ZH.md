文档语言: [English](README.md) | [中文简体](README-ZH.md)

# qrscan

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Pub](https://img.shields.io/pub/v/qrscan.svg)](https://pub.dev/packages/qrscan)

打开相机。扫到一条。字符串回来。关掉。

![qrscan.gif](https://github.com/wechat-program/album/blob/master/pic/cons/qr_scan_demo.gif)

```yaml
dependencies:
  qrscan: ^1.2.1
```

```dart
import 'package:qrscan/qrscan.dart' as scanner;

String? code = await scanner.scan();
String? wechat = await scanner.scan(looks: ScanLooks.wechat);
String? alipay = await scanner.scan(looks: ScanLooks.alipay);
```

Dart 3 / Flutter 3.10+。钉某个提交：

```yaml
dependencies:
  qrscan:
    git:
      url: https://github.com/wu9007/qrcode_scanner.git
      ref: v1.2.1
```

### Android

`minSdk` **21**。相机权限在调用 `scan()` 时申请，不是一安装就抢。不要再加读写存储权限，相册走系统选择器。

宿主 `MainActivity` 必须是 embedding v2（`FlutterActivity`）。

扫码页**跟设备方向**，不再锁死竖屏。用户开了系统旋转锁就停在当前方向。

### iOS

在 `ios/Runner/Info.plist` 增加：

```xml
<key>NSCameraUsageDescription</key>
<string>用于扫描二维码和条码</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>用于从相册识别二维码</string>
```

最低 iOS 12。

iOS 横屏只有宿主 App 的 `UISupportedInterfaceOrientations` 里包含横屏才能转。插件改不了这份 plist。

## 用法

```dart
import 'package:qrscan/qrscan.dart' as scanner;

String? cameraScanResult = await scanner.scan();
String? wechat = await scanner.scan(looks: ScanLooks.wechat);
String? alipay = await scanner.scan(looks: ScanLooks.alipay);
String? branded = await scanner.scan(color: Color(0xFF00C853), hint: '对准条码或二维码');
String? photoScanResult = await scanner.scanPhoto();
String? fromPath = await scanner.scanPath(path);
String? fromBytes = await scanner.scanBytes(bytes);
Uint8List qrPng = await scanner.generateBarCode('https://github.com/wu9007/qrcode_scanner');
```

| 调用 | 返回 |
| --- | --- |
| `scan({color, hint})` | 解码字符串；用户取消为 `null`。只开相机。`color` / `hint` 可选 |
| `scanPhoto()` | 系统相册选图再解码；取消为 `null` |
| `scanPath(path)` | 本地文件 |
| `scanBytes(bytes)` | 图片字节 |
| `generateBarCode(code)` | 二维码 PNG `Uint8List`（UTF-8） |

API 就这些。没有 Widget、没有裁切识别区、没有前后摄像头切换、没有码流、没有格式过滤。框只是瞄准，解码仍是整帧。

### `scan()` 的错误

取消是 `null`。真正失败抛 `PlatformException`：

| Dart 常量 | `code` | 何时 |
| --- | --- | --- |
| `CameraAccessDenied` | `PERMISSION_NOT_GRANTED` | 用户拒绝相机 |
| `CameraStartFailed` | `CAMERA_START_FAILED` | 没有后置摄像头 / 驱动失败 |
| `CameraInUse` | `CAMERA_IN_USE` | 别的 App 占用着相机 |
| `NoActivity` | `NO_ACTIVITY` | 没有 Activity / 弹不出扫描页 |

### 解码

先 QR（带 `TRY_HARDER`），再其他二维，再一维。密集 QR 不会被读成 UPC-E。没有 `formats:` 参数——要限制码制请用 `mobile_scanner`。

Latin-1 二维码（ISO-8859-1、无 ECI）不再被强行当 UTF-8。带高位且合法的 UTF-8 字节仍按 UTF-8（国内常见）。`generateBarCode` 按 UTF-8 写。

解码顺序由 `tool/decoder-harness` 的 63 条样本在 CI 里跑。相册里拍歪/系统存成横图的，Android 和 iOS 都按 90° 重试。iOS 还认 EXIF 方向，反色重试和 Android 一样。

## 什么时候不要用

| 你要的是 | 改用 |
| --- | --- |
| 页面内预览 / 扫码窗 / 叠加层 / 连续扫 / Web / 桌面 | [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) |
| 只扫一维 / 只扫 QR / 要格式列表 | [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) 的 `formats:` |
| 手机或 PDA 的后置摄像头 | 本插件 `scan()` |
| 工业 PDA **激光头**走 Intent 广播 | [`pda_scanner`](https://github.com/wu9007/pda_scanner) |
| 枪设成键盘槽入（HID） | 任何 `TextField`。不装插件 |

三条路互斥：**相机扫一下**（本插件，含 PDA 摄像头）· **厂商激光广播**（`pda_scanner`）· **键盘槽入**（什么都不用）。混用是现场「插件坏了」的第一原因。

## 0.4 复活了什么

| 以前 | 现在 |
| --- | --- |
| 仅 Android，embedding v1 残留 | Android + iOS，embedding v2 |
| JitPack `leyan95:android-zxingLibrary` 经常失踪 | Maven Central `zxing:core:3.5.3` + CameraX 1.4 |
| iOS 是 TODO | Vision 真扫 |
| 乱要存储权限 | 相机 + 系统选择器 |
| SDK `<3.0.0` | Dart 3 / Flutter 3.10+ |
| 没手电的设备点手电崩溃 | CameraX `enableTorch`，没有就 toast |
| 扫码页锁死竖屏（平板 / 横屏坏掉） | 跟设备旋转；叠加框重新居中 |
| 密集 QR 被读成 UPC-E | 先 QR（TRY_HARDER）再一维 |
| Latin-1 QR 被强行 UTF-8 | BYTE_SEGMENTS → ISO-8859-1，合法 UTF-8 除外 |
| 相机占用 / 绑定失败 → 黑屏 Activity | 回 Dart `CAMERA_IN_USE` / `CAMERA_START_FAILED` |

公开 Dart API 没变。两个新错误常量是可选的。

## License

MIT. Created by Shusheng.
