Language: [English](README.md) | [中文简体](README-ZH.md)

# qrscan

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Pub](https://img.shields.io/pub/v/qrscan.svg)](https://pub.dev/packages/qrscan)

Open camera. First decode. String back. Close.

![qrscan.gif](https://github.com/wechat-program/album/blob/master/pic/cons/qr_scan_demo.gif)

```yaml
dependencies:
  qrscan: ^1.1.0
```

```dart
import 'package:qrscan/qrscan.dart' as scanner;

String? code = await scanner.scan();
```

This is **not** an embedded camera widget. Preview / scan window / overlay / torch API / continuous scan → [`mobile_scanner`](https://pub.dev/packages/mobile_scanner).

This is **not** a hardware gun. Keyboard wedge → a `TextField`. OEM broadcast PDA → [`pda_scanner`](https://github.com/wu9007/pda_scanner).

- **Android** CameraX 1.4 + ZXing 3.5.3
- **iOS** AVFoundation + Vision
- Dart 3 / Flutter 3.10+ / Android embedding v2
- Five functions: `scan` / `scanPhoto` / `scanPath` / `scanBytes` / `generateBarCode`

### Android

`minSdk` **21**. Camera permission is requested when `scan()` runs. Do **not** add storage permissions.

Host `MainActivity` must be embedding v2 (`FlutterActivity`). The scanner follows device orientation.

### iOS

```xml
<key>NSCameraUsageDescription</key>
<string>Scan QR codes and barcodes</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Read a code from a photo</string>
```

Minimum iOS 12. Landscape only if the host already lists it in `UISupportedInterfaceOrientations`.

## Usage

```dart
String? cameraScanResult = await scanner.scan();
String? photoScanResult = await scanner.scanPhoto();
String? fromPath = await scanner.scanPath(path);
String? fromBytes = await scanner.scanBytes(bytes);
Uint8List qrPng = await scanner.generateBarCode('https://github.com/wu9007/qrcode_scanner');
```

| Call | Returns |
| --- | --- |
| `scan()` | Decoded string, or `null` if the user cancels. Camera only — not the gallery |
| `scanPhoto()` | System picker → decode, or `null` if cancelled |
| `scanPath(path)` | Decode a local file |
| `scanBytes(bytes)` | Decode image bytes |
| `generateBarCode(code)` | QR PNG as `Uint8List` (UTF-8 payload) |

That is the whole API. There is no widget, no scan-area parameter, no front-camera switch, no stream, no format filter.

### Errors from `scan()`

Cancel is `null`. Real failures throw `PlatformException`:

| Dart constant | `code` | When |
| --- | --- | --- |
| `CameraAccessDenied` | `PERMISSION_NOT_GRANTED` | User denied camera |
| `CameraStartFailed` | `CAMERA_START_FAILED` | No back camera / driver error |
| `CameraInUse` | `CAMERA_IN_USE` | Another app already holds the camera |
| `NoActivity` | `NO_ACTIVITY` | Plugin has no Activity / cannot present |

### Decoder

QR is tried first (with `TRY_HARDER`), then other 2D, then 1D. A dense QR is not returned as UPC-E. There is no `formats:` argument — if you must restrict symbologies, use `mobile_scanner`.

Latin-1 QR (ISO-8859-1, no ECI) is not forced to UTF-8. Byte segments that are valid UTF-8 with high bits stay UTF-8 (typical Chinese QR). `generateBarCode` writes UTF-8.

Decoder order is covered by `tool/decoder-harness` (63 fixtures in CI). Album photos that are stored rotated are retried at 90° steps on Android and iOS. iOS also honors EXIF orientation and inverts like Android.

## When **not** to use this

| You want | Use instead |
| --- | --- |
| Embedded preview / scan window / overlay / continuous scan / web / desktop | [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) |
| Only 1D / only QR / a format list | [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) `formats:` |
| Industrial PDA trigger that broadcasts an Intent | [`pda_scanner`](https://github.com/wu9007/pda_scanner) |
| Gun set to keyboard (HID) wedge | Any `TextField`. No plugin. |

Three mutually exclusive paths: **camera one-shot** (this plugin) · **OEM broadcast** (`pda_scanner`) · **HID keyboard** (nothing). Mixing them is how field apps “break”.

## 0.3 → 0.4

| Before | After |
| --- | --- |
| Android only, embedding v1 leftovers | Android + iOS, embedding v2 |
| `com.github.leyan95:android-zxingLibrary` (JitPack, often missing) | `com.google.zxing:core:3.5.3` + CameraX 1.4 |
| iOS was a TODO | Vision actually scans |
| Storage permissions | Camera + system photo picker |
| SDK `<3.0.0` | Dart 3 / Flutter 3.10+ |
| Torch crash on devices with no flash | CameraX `enableTorch`, toast if unavailable |
| Scanner locked `portrait` (tablets / landscape broken) | Follows device rotation; overlay recenters |
| Dense QR read as UPC-E | QR (TRY_HARDER) before 1D |
| Latin-1 QR forced to UTF-8 | BYTE_SEGMENTS → ISO-8859-1 unless valid UTF-8 |
| Camera busy / bind fail → black Activity | `CAMERA_IN_USE` / `CAMERA_START_FAILED` to Dart |

Public Dart API is unchanged. The two new error constants are optional.

## License

MIT. Created by Shusheng.
