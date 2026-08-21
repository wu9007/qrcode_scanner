import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.Writer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.CodaBarWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.Code93Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.ITFWriter;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.oned.UPCEWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.aztec.AztecWriter;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.pdf417.PDF417Writer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Mirrors com.shinow.qrscan.QrDecoder decode order on the JVM (no Android).
 */
public class DecoderHarness {

    private static final EnumSet<BarcodeFormat> QR_ONLY = EnumSet.of(BarcodeFormat.QR_CODE);
    private static final EnumSet<BarcodeFormat> TWO_D_REST = EnumSet.of(
            BarcodeFormat.DATA_MATRIX, BarcodeFormat.PDF_417, BarcodeFormat.AZTEC);
    private static final EnumSet<BarcodeFormat> ONE_D = EnumSet.of(
            BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93,
            BarcodeFormat.CODABAR, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.ITF);

    private final File outDir;
    private final List<Map<String, Object>> cases = new ArrayList<>();
    private int passed = 0;
    private int failed = 0;

    public DecoderHarness(File outDir) {
        this.outDir = outDir;
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "out");
        out.mkdirs();
        DecoderHarness h = new DecoderHarness(out);
        h.runAll();
        h.writeReport();
        System.out.println("PASS=" + h.passed + " FAIL=" + h.failed);
        if (h.failed > 0) System.exit(1);
    }

    void runAll() throws Exception {
        charset();
        formats();
        field();
        degrade();
        negative();
        encode();
    }

    private void charset() throws Exception {
        qr("hello-ascii", "charset", "https://github.com/wu9007/qrcode_scanner");
        qr("hello-chinese", "charset", "血站发血：A型 RhD+ 400ml");
        qrCharset("latin1-cafe", "Café naïve año", "ISO-8859-1");
        qrCharset("utf8-chinese-hint", "石家庄血站", "UTF-8");
        utf8StoredAsLatin1Bytes("utf8-bytes-no-eci", "石家庄血站");
        qr("json-blood", "charset", "{\"din\":\"G123416123456\",\"abo\":\"A\",\"rhd\":\"+\",\"ml\":400}");
        qr("url-unicode", "charset", "https://example.com/发血?袋=A型&vol=400");
        qr("wifi", "charset", "WIFI:T:WPA;S:BloodLab;P:secret12;;");
        qr("sms-cn", "charset", "SMSTO:10086:取血通知 请到3号窗口");
        qr("dense-base64", "charset", densePayload());
        numericQrNotUpc("numeric-qr", "13258283");
    }

    private void formats() throws Exception {
        oneD("code128-isbt", BarcodeFormat.CODE_128, "A9999B12345601");
        oneD("code39", BarcodeFormat.CODE_39, "ABC123");
        oneD("code93", BarcodeFormat.CODE_93, "CODE93");
        oneD("codabar", BarcodeFormat.CODABAR, "A123456A");
        oneD("ean13", BarcodeFormat.EAN_13, "5901234123457");
        oneD("ean8", BarcodeFormat.EAN_8, "96385074");
        oneD("upca", BarcodeFormat.UPC_A, "012345678905");
        oneD("upce", BarcodeFormat.UPC_E, "01234565");
        oneD("itf", BarcodeFormat.ITF, "12345670");
        twoD("datamatrix", BarcodeFormat.DATA_MATRIX, "DM-FIELD-01");
        twoD("pdf417", BarcodeFormat.PDF_417, "PDF417-BLOOD-BAG");
        twoD("aztec", BarcodeFormat.AZTEC, "AZTEC-OK");
        oneD("code128-gs1-din", BarcodeFormat.CODE_128, "G123416123456");
        oneD("ean13-isbn", BarcodeFormat.EAN_13, "9780201379624");
        oneD("ean13-cola", BarcodeFormat.EAN_13, "5449000000996");
        oneD("code39-mod43", BarcodeFormat.CODE_39, "BLOOD400");
    }

    private void field() throws Exception {
        qr("field-issue-slip", "field", "发血单号 XB-2026-08150017 受血者 床12 A型 RhD+ 红细胞悬液 2U");
        qr("field-wechat-pay", "field", "wxp://f2f0wu9007-blood-station-demo");
        qr("field-alipay", "field", "https://qr.alipay.com/fkx12345abcdef");
        qr("field-bag-url", "field", "https://bs.local/bag?din=G123416123456&abo=A&rhd=POS&vol=400");
        oneD("field-isbt-din", "field", BarcodeFormat.CODE_128, "G123416123456");
        oneD("field-product-code", "field", BarcodeFormat.CODE_128, "E0208V00");
        oneD("field-codabar-bag", "field", BarcodeFormat.CODABAR, "A40123456A");
        oneD("field-ean13-reagent", "field", BarcodeFormat.EAN_13, "6901234567892");
        twoD("field-pdf417-bag", "field", BarcodeFormat.PDF_417, "DIN:G123416123456 ABO:A RH:POS VOL:400");
        twoD("field-dm-short", "field", BarcodeFormat.DATA_MATRIX, "G123416123456");
    }

    private void degrade() throws Exception {
        BufferedImage base = writeQr("https://github.com/wu9007/qrcode_scanner", 360, "UTF-8");
        expect("deg-rotate-90", "degrade", "QR rotated 90°",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(rotate(base, 90), true), rotate(base, 90));
        expect("deg-rotate-180", "degrade", "QR rotated 180°",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(rotate(base, 180), true), rotate(base, 180));
        expect("deg-rotate-270", "degrade", "QR rotated 270°",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(rotate(base, 270), true), rotate(base, 270));
        expect("deg-rotate-15", "degrade", "QR rotated 15°",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(rotate(base, 15), true), rotate(base, 15));
        expect("deg-blur", "degrade", "slight blur",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(blur(base), true), blur(base));
        expect("deg-jpeg40", "degrade", "JPEG quality 0.4",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(jpeg(base, 0.4f), true), jpeg(base, 0.4f));
        expect("deg-dark", "degrade", "darkened 0.45",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(darken(base, 0.45f), true), darken(base, 0.45f));
        expect("deg-low-contrast", "degrade", "gray on gray",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(lowContrast(base), true), lowContrast(base));
        expect("deg-noise", "degrade", "sensor noise",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(noise(base, 28), true), noise(base, 28));
        expect("deg-uneven", "degrade", "uneven lighting",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(uneven(base), true), uneven(base));
        expect("deg-screenshot", "degrade", "phone screenshot chrome",
                "https://github.com/wu9007/qrcode_scanner", decodePlugin(screenshot(base), true), screenshot(base));
        invertQr("deg-invert", "INVERT-OK");
        paddedQr("deg-padded", "PADDED");
        smallQr("deg-tiny", "OK");
        longUrl("deg-long-url", "https://example.com/path/" + "x".repeat(200) + "?q=1");
        BufferedImage barcode = writeFormat(BarcodeFormat.CODE_128, "A9999B12345601", 400, 120);
        expect("deg-128-rotate-90", "degrade", "Code 128 rotated 90° (album)",
                "A9999B12345601", decodePlugin(rotate(barcode, 90), true), rotate(barcode, 90));
        expect("deg-128-jpeg", "degrade", "Code 128 JPEG",
                "A9999B12345601", decodePlugin(jpeg(barcode, 0.5f), true), jpeg(barcode, 0.5f));
        BufferedImage cn = writeQr("血站发血：A型 RhD+ 400ml", 400, "UTF-8");
        expect("deg-cn-jpeg", "degrade", "Chinese QR JPEG",
                "血站发血：A型 RhD+ 400ml", decodePlugin(jpeg(cn, 0.45f), true), jpeg(cn, 0.45f));
        expect("deg-cn-rotate-90", "degrade", "Chinese QR 90°",
                "血站发血：A型 RhD+ 400ml", decodePlugin(rotate(cn, 90), true), rotate(cn, 90));
    }

    private void negative() {
        garbage();
        emptyWhite();
        BufferedImage black = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = black.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 240, 240);
        g.dispose();
        String got = decodePlugin(black, true);
        record("neg-black", "negative", "black frame must be null", null, got, null, got == null);
        save("neg-black", black);
    }

    private void encode() throws Exception {
        generateThenDecodeLikePlugin("enc-roundtrip", "qrscan-1.0");
        generateThenDecodeLikePlugin("enc-chinese", "血站发血：A型 RhD+ 400ml");
        generateThenDecodeLikePlugin("enc-url", "https://github.com/wu9007/qrcode_scanner");
        generateThenDecodeLikePlugin("enc-json", "{\"ok\":true}");
    }

    private void qr(String id, String group, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 400, null);
        expect(id, group, "QR " + group, payload, decodePlugin(img, true), img);
    }

    private void qrCharset(String id, String payload, String charset) throws Exception {
        BufferedImage img = writeQr(payload, 400, charset);
        expect(id, "charset", "QR charset " + charset, payload, decodePlugin(img, true), img);
    }

    private void invertQr(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 320, null);
        BufferedImage inv = invert(img);
        expect(id, "degrade", "inverted QR", payload, decodePlugin(inv, true), inv);
    }

    private void oneD(String id, BarcodeFormat format, String payload) throws Exception {
        oneD(id, "format", format, payload);
    }

    private void oneD(String id, String group, BarcodeFormat format, String payload) throws Exception {
        BufferedImage img = writeFormat(format, payload, 400, 120);
        Result r = decodePluginResult(img, true);
        String got = r == null ? null : textFromResult(r);
        String fmt = r == null ? null : r.getBarcodeFormat().toString();
        boolean ok = payload.equals(got);
        if (!ok && got != null && (format == BarcodeFormat.UPC_E || format == BarcodeFormat.UPC_A)) {
            ok = got.replaceFirst("^0+", "").equals(payload.replaceFirst("^0+", ""))
                    || payload.contains(got) || got.contains(payload);
        }
        record(id, group, "1D " + format, payload, got, fmt, ok);
        save(id, img);
    }

    private void twoD(String id, BarcodeFormat format, String payload) throws Exception {
        twoD(id, "format", format, payload);
    }

    private void twoD(String id, String group, BarcodeFormat format, String payload) throws Exception {
        BufferedImage img = writeFormat(format, payload, 400, 400);
        expect(id, group, "2D " + format, payload, decodePlugin(img, true), img);
    }

    private void utf8StoredAsLatin1Bytes(String id, String payload) throws Exception {
        byte[] utf8 = payload.getBytes(StandardCharsets.UTF_8);
        String asLatin = new String(utf8, StandardCharsets.ISO_8859_1);
        BufferedImage img = writeQr(asLatin, 360, "ISO-8859-1");
        expect(id, "charset", "UTF-8 bytes in QR without ECI", payload, decodePlugin(img, true), img);
    }

    private void garbage() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 200, 200);
        Random rnd = new Random(1);
        for (int i = 0; i < 400; i++) {
            g.setColor(new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255)));
            g.fillRect(rnd.nextInt(200), rnd.nextInt(200), 3, 3);
        }
        g.dispose();
        String got = decodePlugin(img, true);
        record("neg-garbage", "negative", "noise must not invent a code", null, got, null, got == null);
        save("neg-garbage", img);
    }

    private void emptyWhite() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        String got = decodePlugin(img, true);
        record("neg-white", "negative", "blank must be null", null, got, null, got == null);
    }

    private void paddedQr(String id, String payload) throws Exception {
        BufferedImage qr = writeQr(payload, 200, null);
        BufferedImage canvas = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 480, 480);
        g.drawImage(qr, 140, 140, null);
        g.dispose();
        expect(id, "degrade", "QR with large quiet zone", payload, decodePlugin(canvas, true), canvas);
    }

    private void smallQr(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 80, null);
        expect(id, "degrade", "tiny QR 80px", payload, decodePlugin(img, true), img);
    }

    private void longUrl(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 500, "UTF-8");
        expect(id, "degrade", "long URL QR", payload, decodePlugin(img, true), img);
    }

    private void numericQrNotUpc(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 300, null);
        Result r = decodePluginResult(img, true);
        String got = r == null ? null : textFromResult(r);
        String fmt = r == null ? null : r.getBarcodeFormat().toString();
        boolean ok = payload.equals(got) && "QR_CODE".equals(fmt);
        record(id, "charset", "numeric QR must stay QR not UPC-E", payload, got + " [" + fmt + "]", fmt, ok);
        save(id, img);
    }

    private void generateThenDecodeLikePlugin(String id, String payload) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, 400, 400, hints);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
        expect(id, "encode", "encodeQr then decodeBitmap", payload, decodePlugin(img, true), img);
    }

    private void expect(String id, String group, String title, String want, String got, BufferedImage img) {
        boolean ok = (want == null && got == null) || (want != null && want.equals(got));
        record(id, group, title, want, got, null, ok);
        save(id, img);
    }

    private void record(String id, String group, String title, String want, String got, String format, boolean ok) {
        if (ok) passed++;
        else failed++;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("group", group);
        row.put("title", title);
        row.put("want", want);
        row.put("got", got);
        row.put("format", format);
        row.put("ok", ok);
        cases.add(row);
        System.out.println((ok ? "PASS" : "FAIL") + "  [" + group + "] " + id + "  want=" + preview(want)
                + " got=" + preview(got) + (format != null ? " fmt=" + format : ""));
    }

    private static String preview(String s) {
        if (s == null) return "null";
        String t = s.replace("\n", "\\n");
        return t.length() > 60 ? t.substring(0, 57) + "..." : t;
    }

    private static String densePayload() {
        return "5GnnqzQwfYBFmjT0+QypP1Hgll3tQhUCac7eJaIMEg5DlbWA7lMo7C0QB15tKsFa"
                + "EwnUnpFsxTi/xPz489Qwt62MmlQYZoSukTAAtc6dofp7yAdZtWiH77m1vCBjZaPz"
                + "TUBNhyywRsvBfBXi5UzGbp3SHLyTEwnjS4c8f622DI5mF9O+Tqsx9fEQjkBiffir";
    }

    private BufferedImage writeQr(String payload, int size, String charset) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, charset != null ? charset : "UTF-8");
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private BufferedImage writeFormat(BarcodeFormat format, String payload, int w, int h) throws Exception {
        Writer writer = switch (format) {
            case CODE_128 -> new Code128Writer();
            case CODE_39 -> new Code39Writer();
            case CODE_93 -> new Code93Writer();
            case CODABAR -> new CodaBarWriter();
            case EAN_13 -> new EAN13Writer();
            case EAN_8 -> new EAN8Writer();
            case UPC_A -> new UPCAWriter();
            case UPC_E -> new UPCEWriter();
            case ITF -> new ITFWriter();
            case DATA_MATRIX -> new DataMatrixWriter();
            case PDF_417 -> new PDF417Writer();
            case AZTEC -> new AztecWriter();
            default -> new MultiFormatWriter();
        };
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, format == BarcodeFormat.QR_CODE ? 2 : 10);
        BitMatrix matrix = writer.encode(payload, format, w, h, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private static BufferedImage invert(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                dst.setRGB(x, y, (~src.getRGB(x, y)) | 0xFF000000);
            }
        }
        return dst;
    }

    private static BufferedImage rotate(BufferedImage src, double deg) {
        double rad = Math.toRadians(deg);
        double sin = Math.abs(Math.sin(rad));
        double cos = Math.abs(Math.cos(rad));
        int w = src.getWidth();
        int h = src.getHeight();
        int nw = (int) Math.floor(w * cos + h * sin);
        int nh = (int) Math.floor(h * cos + w * sin);
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform at = new AffineTransform();
        at.translate((nw - w) / 2.0, (nh - h) / 2.0);
        at.rotate(rad, w / 2.0, h / 2.0);
        g.drawImage(src, at, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage blur(BufferedImage src) {
        float[] k = {
                1 / 16f, 2 / 16f, 1 / 16f,
                2 / 16f, 4 / 16f, 2 / 16f,
                1 / 16f, 2 / 16f, 1 / 16f
        };
        return new ConvolveOp(new Kernel(3, 3, k), ConvolveOp.EDGE_NO_OP, null).filter(ensureRgb(src), null);
    }

    private static BufferedImage jpeg(BufferedImage src, float q) {
        try {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
            ImageWriter w = it.next();
            ImageWriteParam p = w.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(q);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            w.setOutput(new MemoryCacheImageOutputStream(bos));
            w.write(null, new IIOImage(ensureRgb(src), null, null), p);
            w.dispose();
            return ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
        } catch (Exception e) {
            return src;
        }
    }

    private static BufferedImage darken(BufferedImage src, float factor) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
                int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
                int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
                dst.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return dst;
    }

    private static BufferedImage lowContrast(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int l = ((rgb >> 16) & 0xFF) < 128 ? 110 : 170;
                dst.setRGB(x, y, (l << 16) | (l << 8) | l);
            }
        }
        return dst;
    }

    private static BufferedImage noise(BufferedImage src, int amt) {
        BufferedImage dst = ensureRgb(src);
        Random rnd = new Random(7);
        for (int y = 0; y < dst.getHeight(); y++) {
            for (int x = 0; x < dst.getWidth(); x++) {
                int rgb = dst.getRGB(x, y);
                int n = rnd.nextInt(amt * 2 + 1) - amt;
                int r = clamp(((rgb >> 16) & 0xFF) + n);
                int g = clamp(((rgb >> 8) & 0xFF) + n);
                int b = clamp((rgb & 0xFF) + n);
                dst.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return dst;
    }

    private static BufferedImage uneven(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        int w = src.getWidth();
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < w; x++) {
                float f = 0.55f + 0.7f * (x / (float) w);
                int rgb = src.getRGB(x, y);
                int r = clamp((int) (((rgb >> 16) & 0xFF) * f));
                int g = clamp((int) (((rgb >> 8) & 0xFF) * f));
                int b = clamp((int) ((rgb & 0xFF) * f));
                dst.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return dst;
    }

    private static BufferedImage screenshot(BufferedImage qr) {
        BufferedImage dst = new BufferedImage(360, 640, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setColor(new Color(246, 247, 248));
        g.fillRect(0, 0, 360, 640);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 360, 36);
        g.setColor(new Color(18, 20, 23));
        g.drawString("9:41", 16, 24);
        int x = (360 - 240) / 2;
        g.drawImage(qr, x, 160, 240, 240, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage ensureRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = c.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return c;
        }
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = c.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private String decodePlugin(BufferedImage img, boolean tryHarder) {
        Result r = decodePluginResult(img, tryHarder);
        return r == null ? null : textFromResult(r);
    }

    private Result decodePluginResult(BufferedImage img, boolean tryHarder) {
        return decodeSource(toSource(img), tryHarder);
    }

    /** Same order as QrDecoder.decodeSource, plus 90° retries when tryHarder. */
    private Result decodeSource(LuminanceSource source, boolean tryHarder) {
        Result r = decodePass(source, tryHarder);
        if (r != null || !tryHarder) return r;
        for (int rot = 1; rot <= 3; rot++) {
            source = source.rotateCounterClockwise();
            r = decodePass(source, true);
            if (r != null) return r;
        }
        return null;
    }

    private Result decodePass(LuminanceSource source, boolean tryHarder) {
        Result r = decodeOnceResult(source, QR_ONLY, true);
        if (r != null) return r;
        r = decodeOnceResult(source, TWO_D_REST, tryHarder);
        if (r != null) return r;
        r = decodeOnceResult(source, ONE_D, tryHarder);
        if (r != null) return r;
        LuminanceSource inverted = source.invert();
        r = decodeOnceResult(inverted, QR_ONLY, true);
        if (r != null) return r;
        r = decodeOnceResult(inverted, TWO_D_REST, tryHarder);
        if (r != null) return r;
        return decodeOnceResult(inverted, ONE_D, tryHarder);
    }

    private static LuminanceSource toSource(BufferedImage img) {
        return new BufferedImageLuminanceSource(img);
    }

    private static Result decodeOnceResult(LuminanceSource source, EnumSet<BarcodeFormat> formats,
                                           boolean tryHarder) {
        MultiFormatReader reader = new MultiFormatReader();
        Map<DecodeHintType, Object> map = new EnumMap<>(DecodeHintType.class);
        map.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        if (tryHarder) map.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        if (formats.contains(BarcodeFormat.CODABAR)) {
            map.put(DecodeHintType.RETURN_CODABAR_START_END, Boolean.TRUE);
        }
        reader.setHints(map);
        try {
            return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
        } catch (Exception e) {
            if (!tryHarder) return null;
            try {
                return reader.decodeWithState(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
            } catch (Exception e2) {
                return null;
            }
        } finally {
            reader.reset();
        }
    }

    @SuppressWarnings("unchecked")
    private static String textFromResult(Result result) {
        Map<ResultMetadataType, Object> meta = result.getResultMetadata();
        if (meta != null) {
            Object segs = meta.get(ResultMetadataType.BYTE_SEGMENTS);
            if (segs instanceof List) {
                ByteArrayOutputStream all = new ByteArrayOutputStream();
                for (Object s : (List<?>) segs) {
                    if (s instanceof byte[] b) {
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
            if ((b & 0x80) != 0) return true;
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
            } else if (c >= 0xC2 && c <= 0xDF) need = 1;
            else if (c >= 0xE0 && c <= 0xEF) need = 2;
            else if (c >= 0xF0 && c <= 0xF4) need = 3;
            else return false;
            if (i + need >= bytes.length) return false;
            for (int j = 1; j <= need; j++) {
                if ((bytes[i + j] & 0xC0) != 0x80) return false;
            }
            i += need + 1;
        }
        return true;
    }

    private void save(String id, BufferedImage img) {
        try {
            ImageIO.write(img, "PNG", new File(outDir, id + ".png"));
        } catch (Exception ignored) {
        }
    }

    private void writeReport() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"passed\": ").append(passed).append(",\n");
        sb.append("  \"failed\": ").append(failed).append(",\n");
        sb.append("  \"total\": ").append(passed + failed).append(",\n");
        sb.append("  \"plugin\": \"qrscan\",\n");
        sb.append("  \"version_under_test\": \"1.0.2\",\n");
        sb.append("  \"cases\": [\n");
        for (int i = 0; i < cases.size(); i++) {
            Map<String, Object> c = cases.get(i);
            sb.append("    {");
            sb.append("\"id\":").append(json(c.get("id"))).append(",");
            sb.append("\"group\":").append(json(c.get("group"))).append(",");
            sb.append("\"title\":").append(json(c.get("title"))).append(",");
            sb.append("\"want\":").append(json(c.get("want"))).append(",");
            sb.append("\"got\":").append(json(c.get("got"))).append(",");
            sb.append("\"format\":").append(json(c.get("format"))).append(",");
            sb.append("\"ok\":").append(c.get("ok"));
            sb.append("}");
            if (i < cases.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n");
        Files.writeString(new File(outDir, "decoder-report.json").toPath(), sb.toString());

        StringBuilder md = new StringBuilder();
        md.append("# qrscan decoder report\n\n");
        md.append("- version: 1.0.2\n");
        md.append("- passed: ").append(passed).append("\n");
        md.append("- failed: ").append(failed).append("\n");
        md.append("- total: ").append(passed + failed).append("\n\n");
        String lastGroup = "";
        for (Map<String, Object> c : cases) {
            String g = String.valueOf(c.get("group"));
            if (!g.equals(lastGroup)) {
                md.append("## ").append(g).append("\n\n");
                lastGroup = g;
            }
            md.append("- ").append(Boolean.TRUE.equals(c.get("ok")) ? "PASS" : "FAIL");
            md.append(" `").append(c.get("id")).append("` ").append(c.get("title")).append("\n");
        }
        Files.writeString(new File(outDir, "TEST-REPORT.md").toPath(), md.toString());
    }

    private static String json(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
