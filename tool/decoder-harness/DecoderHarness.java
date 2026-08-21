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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors com.shinow.qrscan.QrDecoder decode order on the JVM (no Android).
 * Generates fixtures, decodes them, writes JSON the preview can show.
 */
public class DecoderHarness {

    private static final EnumSet<BarcodeFormat> QR_ONLY = EnumSet.of(BarcodeFormat.QR_CODE);
    private static final EnumSet<BarcodeFormat> TWO_D_REST = EnumSet.of(
            BarcodeFormat.DATA_MATRIX, BarcodeFormat.PDF_417, BarcodeFormat.AZTEC);
    private static final EnumSet<BarcodeFormat> ONE_D = EnumSet.of(
            BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93,
            BarcodeFormat.CODABAR, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.ITF);
    private static final EnumSet<BarcodeFormat> ALL = EnumSet.copyOf(QR_ONLY);
    static {
        ALL.addAll(TWO_D_REST);
        ALL.addAll(ONE_D);
    }

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
        if (h.failed > 0) {
            System.exit(1);
        }
    }

    void runAll() throws Exception {
        qrRoundtrip("hello-ascii", "https://github.com/wu9007/qrcode_scanner");
        qrRoundtrip("hello-chinese", "血站发血：A型 RhD+ 400ml");
        qrCharset("latin1-cafe", "Café naïve año", "ISO-8859-1");
        qrCharset("utf8-chinese-hint", "石家庄血站", "UTF-8");
        qrRoundtrip("dense-base64", densePayload());
        invertQr("invert-white-on-black", "INVERT-OK");
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
        oldVsNewDense();
        garbage();
        emptyWhite();
        paddedQr("qr-with-quiet-zone-and-margin", "PADDED");
        smallQr("tiny-version1", "OK");
        longUrl("long-url", "https://example.com/path/" + "x".repeat(200) + "?q=1");
        numericQrNotUpc("numeric-qr", "13258283");
        utf8StoredAsLatin1Bytes("utf8-bytes-no-eci", "石家庄血站");
        generateThenDecodeLikePlugin("plugin-encode-roundtrip", "qrscan-1.0");
        generateThenDecodeLikePlugin("plugin-encode-chinese", "血站发血：A型 RhD+ 400ml");
    }

    private void qrRoundtrip(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 400, null);
        expect(id, "QR roundtrip", payload, decodePlugin(img, true), img);
    }

    private void qrCharset(String id, String payload, String charset) throws Exception {
        BufferedImage img = writeQr(payload, 400, charset);
        expect(id, "QR charset " + charset, payload, decodePlugin(img, true), img);
    }

    private void invertQr(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 320, null);
        BufferedImage inv = invert(img);
        save(id, inv);
        expect(id, "inverted QR", payload, decodePlugin(inv, true), inv);
    }

    private void oneD(String id, BarcodeFormat format, String payload) throws Exception {
        BufferedImage img = writeFormat(format, payload, 400, 120);
        Result r = decodePluginResult(img, true);
        String got = r == null ? null : textFromResult(r);
        String fmt = r == null ? null : r.getBarcodeFormat().toString();
        boolean ok = payload.equals(got);
        if (!ok && got != null && (format == BarcodeFormat.UPC_E || format == BarcodeFormat.UPC_A)) {
            ok = got.replaceFirst("^0+", "").equals(payload.replaceFirst("^0+", ""))
                    || payload.contains(got) || got.contains(payload);
        }
        record(id, "1D " + format, payload, got, fmt, ok);
        save(id, img);
    }

    private void utf8StoredAsLatin1Bytes(String id, String payload) throws Exception {
        byte[] utf8 = payload.getBytes(StandardCharsets.UTF_8);
        String asLatin = new String(utf8, StandardCharsets.ISO_8859_1);
        BufferedImage img = writeQr(asLatin, 360, "ISO-8859-1");
        expect(id, "UTF-8 bytes in QR without ECI (China default)", payload, decodePlugin(img, true), img);
    }

    private void twoD(String id, BarcodeFormat format, String payload) throws Exception {
        BufferedImage img = writeFormat(format, payload, 400, 400);
        expect(id, "2D " + format, payload, decodePlugin(img, true), img);
    }

    private void oldVsNewDense() throws Exception {
        String payload = densePayload();
        BufferedImage img = writeQr(payload, 600, null);
        save("dense-old-vs-new", img);
        String plugin = decodePlugin(img, true);
        String oldAll = decodeOnce(toSource(img), ALL, true);
        boolean pluginOk = payload.equals(plugin);
        boolean stolen = oldAll != null && oldAll.length() <= 12 && !payload.equals(oldAll);
        record("dense-plugin", "dense QR via plugin order", payload, plugin,
                pluginOk ? "QR_CODE" : "?", pluginOk);
        record("dense-legacy-all-formats", "legacy all-formats-at-once",
                "must-not-be-short-upc", oldAll,
                stolen ? "STOLEN" : "ok-or-full",
                !stolen || pluginOk);
        // The 1.0 bar: plugin must return the full payload. Legacy steal is informational.
        if (!pluginOk) {
            // already counted in record
        }
    }

    private void garbage() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 200, 200);
        for (int i = 0; i < 400; i++) {
            g.setColor(new Color((i * 37) % 255, (i * 19) % 255, (i * 11) % 255));
            g.fillRect((i * 13) % 200, (i * 17) % 200, 3, 3);
        }
        g.dispose();
        String got = decodePlugin(img, true);
        record("garbage-noise", "noise must not invent a code", null, got, null, got == null);
        save("garbage-noise", img);
    }

    private void emptyWhite() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        String got = decodePlugin(img, true);
        record("empty-white", "blank must be null", null, got, null, got == null);
    }

    private void paddedQr(String id, String payload) throws Exception {
        BufferedImage qr = writeQr(payload, 200, null);
        BufferedImage canvas = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 480, 480);
        g.drawImage(qr, 140, 140, null);
        g.dispose();
        expect(id, "QR with large quiet zone", payload, decodePlugin(canvas, true), canvas);
    }

    private void smallQr(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 80, null);
        expect(id, "tiny QR 80px", payload, decodePlugin(img, true), img);
    }

    private void longUrl(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 500, "UTF-8");
        expect(id, "long URL QR", payload, decodePlugin(img, true), img);
    }

    private void numericQrNotUpc(String id, String payload) throws Exception {
        BufferedImage img = writeQr(payload, 300, null);
        Result r = decodePluginResult(img, true);
        String got = r == null ? null : textFromResult(r);
        String fmt = r == null ? null : r.getBarcodeFormat().toString();
        boolean ok = payload.equals(got) && "QR_CODE".equals(fmt);
        record(id, "numeric QR must stay QR not UPC-E", payload, got + " [" + fmt + "]", fmt, ok);
        save(id, img);
        String oldAll = decodeOnce(toSource(img), ALL, true);
        record(id + "-legacy", "legacy all-formats on numeric QR", payload, oldAll, null, true);
    }

    private void generateThenDecodeLikePlugin(String id, String payload) throws Exception {
        // Same as QrDecoder.encodeQr: QRCodeWriter default hints, black/white PNG
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, 400, 400, hints);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
        expect(id, "encodeQr then decodeBitmap", payload, decodePlugin(img, true), img);
    }

    private void expect(String id, String title, String want, String got, BufferedImage img) {
        boolean ok = (want == null && got == null) || (want != null && want.equals(got));
        record(id, title, want, got, null, ok);
        save(id, img);
    }

    private void record(String id, String title, String want, String got, String format, boolean ok) {
        if (ok) passed++;
        else failed++;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("title", title);
        row.put("want", want);
        row.put("got", got);
        row.put("format", format);
        row.put("ok", ok);
        cases.add(row);
        System.out.println((ok ? "PASS" : "FAIL") + "  " + id + "  want=" + preview(want) + " got=" + preview(got)
                + (format != null ? " fmt=" + format : ""));
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
                int rgb = src.getRGB(x, y);
                dst.setRGB(x, y, (~rgb) | 0xFF000000);
            }
        }
        return dst;
    }

    private String decodePlugin(BufferedImage img, boolean tryHarder) {
        Result r = decodePluginResult(img, tryHarder);
        return r == null ? null : textFromResult(r);
    }

    private Result decodePluginResult(BufferedImage img, boolean tryHarder) {
        LuminanceSource source = toSource(img);
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

    private static String decodeOnce(LuminanceSource source, EnumSet<BarcodeFormat> formats, boolean tryHarder) {
        Result r = decodeOnceResult(source, formats, tryHarder);
        return r == null ? null : textFromResult(r);
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
                return reader.decodeWithState(new BinaryBitmap(
                        new com.google.zxing.common.GlobalHistogramBinarizer(source)));
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
        sb.append("  \"version_under_test\": \"1.0.0\",\n");
        sb.append("  \"cases\": [\n");
        for (int i = 0; i < cases.size(); i++) {
            Map<String, Object> c = cases.get(i);
            sb.append("    {");
            sb.append("\"id\":").append(json(c.get("id"))).append(",");
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
    }

    private static String json(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
