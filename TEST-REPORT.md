# qrscan decoder report

- version: 1.0.2
- passed: 63
- failed: 0
- total: 63

## charset

- PASS `hello-ascii` QR charset
- PASS `hello-chinese` QR charset
- PASS `latin1-cafe` QR charset ISO-8859-1
- PASS `utf8-chinese-hint` QR charset UTF-8
- PASS `utf8-bytes-no-eci` UTF-8 bytes in QR without ECI
- PASS `json-blood` QR charset
- PASS `url-unicode` QR charset
- PASS `wifi` QR charset
- PASS `sms-cn` QR charset
- PASS `dense-base64` QR charset
- PASS `numeric-qr` numeric QR must stay QR not UPC-E
## format

- PASS `code128-isbt` 1D CODE_128
- PASS `code39` 1D CODE_39
- PASS `code93` 1D CODE_93
- PASS `codabar` 1D CODABAR
- PASS `ean13` 1D EAN_13
- PASS `ean8` 1D EAN_8
- PASS `upca` 1D UPC_A
- PASS `upce` 1D UPC_E
- PASS `itf` 1D ITF
- PASS `datamatrix` 2D DATA_MATRIX
- PASS `pdf417` 2D PDF_417
- PASS `aztec` 2D AZTEC
- PASS `code128-gs1-din` 1D CODE_128
- PASS `ean13-isbn` 1D EAN_13
- PASS `ean13-cola` 1D EAN_13
- PASS `code39-mod43` 1D CODE_39
## field

- PASS `field-issue-slip` QR field
- PASS `field-wechat-pay` QR field
- PASS `field-alipay` QR field
- PASS `field-bag-url` QR field
- PASS `field-isbt-din` 1D CODE_128
- PASS `field-product-code` 1D CODE_128
- PASS `field-codabar-bag` 1D CODABAR
- PASS `field-ean13-reagent` 1D EAN_13
- PASS `field-pdf417-bag` 2D PDF_417
- PASS `field-dm-short` 2D DATA_MATRIX
## degrade

- PASS `deg-rotate-90` QR rotated 90°
- PASS `deg-rotate-180` QR rotated 180°
- PASS `deg-rotate-270` QR rotated 270°
- PASS `deg-rotate-15` QR rotated 15°
- PASS `deg-blur` slight blur
- PASS `deg-jpeg40` JPEG quality 0.4
- PASS `deg-dark` darkened 0.45
- PASS `deg-low-contrast` gray on gray
- PASS `deg-noise` sensor noise
- PASS `deg-uneven` uneven lighting
- PASS `deg-screenshot` phone screenshot chrome
- PASS `deg-invert` inverted QR
- PASS `deg-padded` QR with large quiet zone
- PASS `deg-tiny` tiny QR 80px
- PASS `deg-long-url` long URL QR
- PASS `deg-128-rotate-90` Code 128 rotated 90° (album)
- PASS `deg-128-jpeg` Code 128 JPEG
- PASS `deg-cn-jpeg` Chinese QR JPEG
- PASS `deg-cn-rotate-90` Chinese QR 90°
## negative

- PASS `neg-garbage` noise must not invent a code
- PASS `neg-white` blank must be null
- PASS `neg-black` black frame must be null
## encode

- PASS `enc-roundtrip` encodeQr then decodeBitmap
- PASS `enc-chinese` encodeQr then decodeBitmap
- PASS `enc-url` encodeQr then decodeBitmap
- PASS `enc-json` encodeQr then decodeBitmap
