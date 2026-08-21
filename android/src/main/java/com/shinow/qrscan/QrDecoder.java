package com.shinow.qrscan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

final class QrDecoder {

    /** QR alone, TRY_HARDER. Dense payloads (issue #85) must beat UPC-E. */
    private static final EnumSet<BarcodeFormat> QR_ONLY = EnumSet.of(BarcodeFormat.QR_CODE);
    private static final EnumSet<BarcodeFormat> TWO_D_REST = EnumSet.of(
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.PDF_417,
            BarcodeFormat.AZTEC
    );
    private static final EnumSet<BarcodeFormat> ONE_D = EnumSet.of(
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODABAR,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.ITF
    );

    private QrDecoder() {
    }

    @Nullable
    static String decodeImageProxy(ImageProxy image) {
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ByteBuffer buffer = yPlane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = yPlane.getRowStride();
        byte[] data = new byte[width * height];
        int offset = 0;
        for (int row = 0; row < height; row++) {
            buffer.position(row * rowStride);
            buffer.get(data, offset, width);
            offset += width;
        }
        int rotation = image.getImageInfo().getRotationDegrees();
        if (rotation == 90 || rotation == 270) {
            data = rotateY90(data, width, height, rotation);
            int tmp = width;
            width = height;
            height = tmp;
        } else if (rotation == 180) {
            data = rotateY180(data, width, height);
        }
        return decodeYuv(data, width, height);
    }

    @Nullable
    static String decodeYuv(byte[] yuv, int width, int height) {
        LuminanceSource source = new PlanarYUVLuminanceSource(
                yuv, width, height, 0, 0, width, height, false);
        return decodeSource(source, false);
    }

    @Nullable
    static String decodeBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return decodeSource(new RGBLuminanceSource(width, height, pixels), true);
    }

    @Nullable
    static String decodeFile(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, 1280, 1280);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        try {
            return decodeBitmap(bitmap);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    @Nullable
    static String decodeBytes(byte[] bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options, 1280, 1280);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        try {
            return decodeBitmap(bitmap);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    static byte[] encodeQr(String content, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        bitmap.recycle();
        return stream.toByteArray();
    }

    @Nullable
    private static String decodeSource(LuminanceSource source, boolean tryHarder) {
        String text = decodeOnce(source, QR_ONLY, true);
        if (text != null) {
            return text;
        }
        text = decodeOnce(source, TWO_D_REST, tryHarder);
        if (text != null) {
            return text;
        }
        text = decodeOnce(source, ONE_D, tryHarder);
        if (text != null) {
            return text;
        }
        LuminanceSource inverted = source.invert();
        text = decodeOnce(inverted, QR_ONLY, true);
        if (text != null) {
            return text;
        }
        text = decodeOnce(inverted, TWO_D_REST, tryHarder);
        if (text != null) {
            return text;
        }
        return decodeOnce(inverted, ONE_D, tryHarder);
    }

    @Nullable
    private static String decodeOnce(LuminanceSource source, EnumSet<BarcodeFormat> formats,
                                     boolean tryHarder) {
        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints(formats, tryHarder));
        try {
            Result result = reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
            return textFromResult(result);
        } catch (Exception ignored) {
            return null;
        } finally {
            reader.reset();
        }
    }

    private static Map<DecodeHintType, Object> hints(EnumSet<BarcodeFormat> formats,
                                                     boolean tryHarder) {
        Map<DecodeHintType, Object> map = new EnumMap<>(DecodeHintType.class);
        map.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        if (tryHarder) {
            map.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        }
        return map;
    }

    /**
     * Do not force UTF-8. QR byte mode without ECI is ISO-8859-1 (#40).
     * If BYTE_SEGMENTS are valid UTF-8 with high bits, prefer UTF-8 (Chinese QR).
     */
    @SuppressWarnings("unchecked")
    private static String textFromResult(Result result) {
        Map<ResultMetadataType, Object> meta = result.getResultMetadata();
        if (meta != null) {
            Object segs = meta.get(ResultMetadataType.BYTE_SEGMENTS);
            if (segs instanceof List) {
                ByteArrayOutputStream all = new ByteArrayOutputStream();
                for (Object s : (List<?>) segs) {
                    if (s instanceof byte[]) {
                        byte[] b = (byte[]) s;
                        all.write(b, 0, b.length);
                    }
                }
                byte[] bytes = all.toByteArray();
                if (bytes.length > 0) {
                    if (hasHighBit(bytes) && isValidUtf8(bytes)) {
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                    return new String(bytes, StandardCharsets.ISO_8859_1);
                }
            }
        }
        return result.getText();
    }

    private static boolean hasHighBit(byte[] bytes) {
        for (byte b : bytes) {
            if ((b & 0x80) != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidUtf8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int c = bytes[i] & 0xFF;
            int need;
            if (c <= 0x7F) {
                i++;
                continue;
            } else if (c >= 0xC2 && c <= 0xDF) {
                need = 1;
            } else if (c >= 0xE0 && c <= 0xEF) {
                need = 2;
            } else if (c >= 0xF0 && c <= 0xF4) {
                need = 3;
            } else {
                return false;
            }
            if (i + need >= bytes.length) {
                return false;
            }
            for (int j = 1; j <= need; j++) {
                if ((bytes[i + j] & 0xC0) != 0x80) {
                    return false;
                }
            }
            i += need + 1;
        }
        return true;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqW, int reqH) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqH || width > reqW) {
            int halfH = height / 2;
            int halfW = width / 2;
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static byte[] rotateY90(byte[] data, int width, int height, int rotation) {
        byte[] rotated = new byte[width * height];
        int i = 0;
        if (rotation == 90) {
            for (int x = 0; x < width; x++) {
                for (int y = height - 1; y >= 0; y--) {
                    rotated[i++] = data[y * width + x];
                }
            }
        } else {
            for (int x = width - 1; x >= 0; x--) {
                for (int y = 0; y < height; y++) {
                    rotated[i++] = data[y * width + x];
                }
            }
        }
        return rotated;
    }

    private static byte[] rotateY180(byte[] data, int width, int height) {
        byte[] rotated = new byte[width * height];
        int n = width * height;
        for (int i = 0; i < n; i++) {
            rotated[n - 1 - i] = data[i];
        }
        return rotated;
    }
}
