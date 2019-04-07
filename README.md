# QR Code Scanner
    A Flutter plugin 🛠 to scanning. Ready for Android 🚀
  
[![License][license-image]][license-url] 

base on ZXing [github](https://github.com/yipianfengye/android-zxingLibrary)

#### permission：
- `<uses-permission android:name="android.permission.INTERNET"/>`
- `<uses-permission android:name="android.permission.CAMERA" />`
- `<uses-permission android:name="android.permission.VIBRATE"/>`

## Installation

Add this to your package's pubspec.yaml file:

```
dependencies:
 qrscan: ^0.0.1
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

Created by Fabricio Serralvo and Marcos Aoki.

[license-image]: https://img.shields.io/badge/License-MIT-blue.svg
[license-url]: LICENSE
