Language: [English](README.md) | [中文简体](README-ZH.md)

# qrscan

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Pub](https://img.shields.io/pub/v/qrscan.svg)](https://pub.dev/packages/qrscan)

One-shot Flutter QR / barcode plugin: **open camera, first decode, return a string, close.** Also album / path / bytes decode, and QR PNG generation.

This is **not** an embedded camera widget. If you need a preview, scan window, overlay, torch API, or continuous scan, use [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) — that race is already over.

This is **not** a hardware gun. Keyboard wedge → a `TextField`. OEM broadcast PDA → [`pda_scanner`](https://github.com/wu9007/pda_scanner). Do not open the camera for those.

- **Android** CameraX 1.4 + ZXing 3.5.3 (QR, Code 128/39/93, EAN, UPC, ITF, Data Matrix, PDF417, Aztec, Codabar)
- **iOS** AVFoundation + Vision
- Dart 3 / Flutter 3.10+ / Android embedding v2
- Public Dart API is the same five functions as 0.3.x

## Install

**pub.dev still lists 0.3.3.** GitHub tag `0.4.0` is the current build. `qrscan: ^0.4.0` will not resolve on pub yet.

```yaml
dependencies:
  qrscan:
    git:
      url: https://github.com/wu9007/qrcode_scanner.git
      ref: 0.4.0
```

After 0.4.0 is on pub:

```yaml
dependencies:
  qrscan: ^0.4.0
```

### Android

`minSdk` **21**. Camera permission is requested when `scan()` runs, not at install. Do **not** add storage permissions — album picking uses the system picker.

Host `MainActivity` must be embedding v2 (`FlutterActivity`).

The scanner activity **follows the device orientation**. It does not lock portrait. If the user has system rotation lock on, the scanner stays put.

### iOS

Add to `ios/Runner/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Scan QR codes and barcodes</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Read a code from a photo</string>
```

Minimum iOS 12.

Landscape on iOS only works if the host app already lists landscape in `UISupportedInterfaceOrientations`. A plugin cannot override that plist.

## Usage

```dart
import 'package:qrscan/qrscan.dart' as scanner;

String? cameraScanResult = await scanner.scan();
String? photoScanResult = await scanner.scanPhoto();
String? fromPath = await scanner.scanPath(path);
String? fromBytes = await scanner.scanBytes(bytes);
Uint8List qrPng = await scanner.generateBarCode('https://github.com/wu9007/qrcode_scanner');
```

| Call | Returns |
| --- | --- |
| `scan()` | Decoded string, or `null` if the user cancels |
| `scanPhoto()` | System picker → decode, or `null` if cancelled |
| `scanPath(path)` | Decode a local file |
| `scanBytes(bytes)` | Decode image bytes |
| `generateBarCode(code)` | QR PNG as `Uint8List` |

That is the whole API. There is no widget, no scan-area parameter, no front-camera switch, no stream, no format filter.

### Errors from `scan()`

Cancel is `null`. Real failures throw `PlatformException`:

| Dart constant | `code` | When |
| --- | --- | --- |
| `CameraAccessDenied` | `PERMISSION_NOT_GRANTED` | User denied camera |
| `CameraStartFailed` | `CAMERA_START_FAILED` | No back camera / driver error |
| `CameraInUse` | `CAMERA_IN_USE` | Another app already holds the camera |

### Decoder

QR is tried first (with `TRY_HARDER`), then other 2D, then 1D. A dense QR is not returned as UPC-E. There is no `formats:` argument — if you must restrict symbologies, use `mobile_scanner`.

Latin-1 QR (ISO-8859-1, no ECI) is not forced to UTF-8. Byte segments that are valid UTF-8 with high bits stay UTF-8 (typical Chinese QR).

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
