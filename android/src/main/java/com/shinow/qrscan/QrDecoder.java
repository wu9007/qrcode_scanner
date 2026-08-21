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
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

final class QrDecoder {

    private static final MultiFormatReader READER = new MultiFormatReader();
    private static final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);
    private static final Map<DecodeHintType, Object> HARD_HINTS = new EnumMap<>(DecodeHintType.class);

    static {
        EnumSet<BarcodeFormat> formats = EnumSet.of(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODABAR,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.ITF,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.PDF_417,
                BarcodeFormat.AZTEC
        );
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        HARD_HINTS.putAll(HINTS);
        HARD_HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        READER.setHints(HINTS);
    }

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
        BinaryBitmap binary = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result;
            if (tryHarder) {
                MultiFormatReader hard = new MultiFormatReader();
                hard.setHints(HARD_HINTS);
                result = hard.decode(binary);
            } else {
                result = READER.decodeWithState(binary);
            }
            return result.getText();
        } catch (NotFoundException ignored) {
            try {
                Result result = READER.decodeWithState(
                        new BinaryBitmap(new HybridBinarizer(source.invert())));
                return result.getText();
            } catch (Exception ignored2) {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            READER.reset();
        }
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
