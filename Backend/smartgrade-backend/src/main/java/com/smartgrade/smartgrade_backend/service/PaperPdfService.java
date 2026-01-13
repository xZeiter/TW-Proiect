package com.smartgrade.smartgrade_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrade.smartgrade_backend.entity.QuizEntity;
import com.smartgrade.smartgrade_backend.entity.SheetEntity;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class PaperPdfService {

    private final ObjectMapper objectMapper;

    public PaperPdfService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path generateAndStorePdf(String ownerEmail,
                                    QuizEntity quiz,
                                    int layoutVersion,
                                    String layoutJson,
                                    List<SheetEntity> sheets) {
        try {
            if (sheets == null || sheets.isEmpty()) {
                throw new RuntimeException("No sheets to generate");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> layout = objectMapper.readValue(layoutJson, Map.class);

            Path dir = Paths.get("storage", "papers",
                    sanitize(ownerEmail),
                    sanitize(quiz.getId()),
                    String.valueOf(layoutVersion)
            );
            Files.createDirectories(dir);

            Path pdfPath = dir.resolve("papers.pdf");

            try (PDDocument doc = new PDDocument()) {
                for (SheetEntity sheet : sheets) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        drawPage(cs, doc, page, quiz, layoutVersion, layout, sheet.getId());
                    }
                }
                doc.save(pdfPath.toFile());
            }

            return pdfPath;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void drawPage(PDPageContentStream cs,
                          PDDocument doc,
                          PDPage page,
                          QuizEntity quiz,
                          int layoutVersion,
                          Map<String, Object> layout,
                          Long sheetId) throws Exception {

        final float pageH = page.getMediaBox().getHeight();

        // Title
        drawText(cs,
                "SMARTGRADE - " + quiz.getName() + " (" + quiz.getId() + ")",
                PDType1Font.HELVETICA_BOLD, 14,
                50, pageH - 42
        );

        // Anchors
        Object anchorsObj = layout.get("anchors");
        if (anchorsObj instanceof List<?> anchors) {
            for (Object ao : anchors) {
                if (!(ao instanceof Map<?, ?> a)) continue;
                float x = num(a.get("x"));
                float y = num(a.get("y"));
                float size = num(a.get("size"));
                drawFilledSquare(cs, x - size / 2f, pageH - y - size / 2f, size);
            }
        }

        // QR
        Map<String, Object> qr = (Map<String, Object>) layout.get("qr");
        if (qr == null) throw new RuntimeException("layout.qr missing");

        int qrSize = (int) num(qr.get("size"));
        float qrX = num(qr.get("x"));
        float qrY = num(qr.get("y"));

        String payload = "SG|q=" + quiz.getId() + "|s=" + sheetId + "|v=" + layoutVersion;

        BufferedImage qrImg = makeQr(payload, qrSize);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImg, "png", baos);

        PDImageXObject pdImg = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "qr");
        cs.drawImage(pdImg, qrX, pageH - qrY - qrSize, qrSize, qrSize);

        // Labels
        drawLabelsFromLayout(cs, pageH, layout);

        // EXT ID
        Map<String, Object> extId = (Map<String, Object>) layout.get("extId");
        if (extId != null) {
            Map<String, Object> box = (Map<String, Object>) extId.get("box");
            if (box != null) {
                float bx = num(box.get("x"));
                float by = num(box.get("y"));
                float bw = num(box.get("w"));
                float bh = num(box.get("h"));
                drawRect(cs, bx, pageH - by - bh, bw, bh);
            }

            drawExtIdGuides(cs, pageH, extId);

            Object cellsObj = extId.get("cells");
            if (cellsObj instanceof List<?> cells) {
                for (Object co : cells) {
                    if (!(co instanceof Map<?, ?> cell)) continue;
                    Object bubblesObj = cell.get("bubbles");
                    if (!(bubblesObj instanceof List<?> bubbles)) continue;

                    for (Object bo : bubbles) {
                        if (!(bo instanceof Map<?, ?> b)) continue;
                        float x = num(b.get("x"));
                        float y = num(b.get("y"));
                        float r = num(b.get("r"));
                        drawCircle(cs, x, pageH - y, r);
                    }
                }
            }
        }

        // NAME box
        Map<String, Object> name = (Map<String, Object>) layout.get("name");
        if (name != null) {
            Map<String, Object> nameBox = (Map<String, Object>) name.get("box");
            if (nameBox != null) {
                float nbx = num(nameBox.get("x"));
                float nby = num(nameBox.get("y"));
                float nbw = num(nameBox.get("w"));
                float nbh = num(nameBox.get("h"));
                drawRect(cs, nbx, pageH - nby - nbh, nbw, nbh);
            }
        }

        // Questions
Object questionsObj = layout.get("questions");
if (!(questionsObj instanceof List<?> questions)) return;

for (Object qo : questions) {
    if (!(qo instanceof Map<?, ?> q)) continue;

    int qIndex = (int) num(q.get("qIndex"));
    Object optionsObj = q.get("options");

    // daca lipseste complet, tratam ca lista goala
    List<?> options = (optionsObj instanceof List<?> o) ? o : List.of();

    if (options.isEmpty()) {
        // ✅ deseneaza doar numarul intrebarii + text "fara raspunsuri"
        Map<String, Object> rowBox = null;
        Object rowBoxObj = q.get("rowBox");
        if (rowBoxObj instanceof Map<?, ?> rb) {
            rowBox = (Map<String, Object>) rb;
        }

        float yTopBased;
        if (rowBox != null) {
            yTopBased = num(rowBox.get("y")) + 14; // aproximativ centrul randului
        } else {
            // fallback: pune-le una sub alta
            yTopBased = 300 + qIndex * 28;
        }

        float yPdf = pageH - yTopBased;

        drawText(cs, (qIndex + 1) + ")", PDType1Font.HELVETICA, 10, 50, yPdf);

        drawText(cs, "(fara raspunsuri)", PDType1Font.HELVETICA, 9, 80, yPdf);

        continue;
    }

    Object first = options.get(0);
    if (!(first instanceof Map<?, ?> opt0)) continue;
    float rowY = num(opt0.get("y"));

    // question number
    drawText(cs, (qIndex + 1) + ")", PDType1Font.HELVETICA, 10, 50, pageH - rowY - 3);

    for (Object oo : options) {
        if (!(oo instanceof Map<?, ?> opt)) continue;

        float x = num(opt.get("x"));
        float y = num(opt.get("y"));
        float r = num(opt.get("r"));
        String label = String.valueOf(opt.get("label"));

        drawCircle(cs, x, pageH - y, r);

        float labelX = x - 2;
        float labelY = (pageH - y) - (r + 10);
        drawText(cs, label, PDType1Font.HELVETICA, 8, labelX, labelY);
    }
}

    }

    // ==================== EXT ID guides ====================
    @SuppressWarnings("unchecked")
    private void drawExtIdGuides(PDPageContentStream cs, float pageH, Map<String, Object> extId) throws IOException {
        if (extId == null) return;

        Integer length = null;
        Object lenObj = extId.get("length");
        if (lenObj instanceof Number n) length = n.intValue();

        String mode = String.valueOf(extId.getOrDefault("mode", "DIGITS_ONLY"));

        Object cellsObj = extId.get("cells");
        if (!(cellsObj instanceof List<?> cells) || cells.isEmpty()) return;

        Object c0 = cells.get(0);
        if (!(c0 instanceof Map<?, ?> firstCell)) return;

        Object fbObj = firstCell.get("bubbles");
        if (!(fbObj instanceof List<?> firstBubbles) || firstBubbles.isEmpty()) return;

        Object b0 = firstBubbles.get(0);
        if (!(b0 instanceof Map<?, ?> firstBubble)) return;

        float x0 = num(firstBubble.get("x"));
        float y0 = num(firstBubble.get("y"));
        float r  = num(firstBubble.get("r"));

        float colStep = 22f;
        if (cells.size() > 1) {
            Object c1 = cells.get(1);
            if (c1 instanceof Map<?, ?> cell1) {
                Object b1obj = cell1.get("bubbles");
                if (b1obj instanceof List<?> b1list && !b1list.isEmpty()) {
                    Object b1 = b1list.get(0);
                    if (b1 instanceof Map<?, ?> bubble1) {
                        colStep = Math.max(12f, num(bubble1.get("x")) - x0);
                    }
                }
            }
        }

        float rowStep = 14f;
        if (firstBubbles.size() > 1) {
            Object b2 = firstBubbles.get(1);
            if (b2 instanceof Map<?, ?> secondBubble) {
                rowStep = Math.max(8f, num(secondBubble.get("y")) - y0);
            }
        }

        int L = (length != null) ? length : cells.size();

        float headerYOffset = (r + 10f);
        for (int pos = 0; pos < L; pos++) {
            float cx = x0 + pos * colStep;
            String txt = String.valueOf(pos + 1);

            float shift = (txt.length() == 1) ? 2.0f : 4.0f;
            drawText(cs, txt, PDType1Font.HELVETICA_BOLD, 7,
                    cx - shift, (pageH - y0) + headerYOffset);
        }

        List<String> symbols = new ArrayList<>();
        if ("ALPHANUMERIC".equalsIgnoreCase(mode)) {
            for (char c = '0'; c <= '9'; c++) symbols.add(String.valueOf(c));
            for (char c = 'A'; c <= 'Z'; c++) symbols.add(String.valueOf(c));
        } else {
            for (char c = '0'; c <= '9'; c++) symbols.add(String.valueOf(c));
        }

        float leftLabelX = x0 - (r + 14f);

        for (int si = 0; si < symbols.size(); si++) {
            float cy = y0 + si * rowStep;
            String sym = symbols.get(si);

            drawText(cs, sym, PDType1Font.HELVETICA, 7,
                    leftLabelX, (pageH - cy) - 2f);
        }
    }

    // ==================== Labels ====================
    @SuppressWarnings("unchecked")
    private void drawLabelsFromLayout(PDPageContentStream cs, float pageH, Map<String, Object> layout) throws IOException {
        Object labelsObj = layout.get("labels");
        if (!(labelsObj instanceof Map<?, ?> labels)) return;

        drawOneLabel(cs, pageH, (Map<String, Object>) labels.get("extId"));
        drawOneLabel(cs, pageH, (Map<String, Object>) labels.get("name"));
        drawOneLabel(cs, pageH, (Map<String, Object>) labels.get("questions"));
    }

    private void drawOneLabel(PDPageContentStream cs, float pageH, Map<String, Object> label) throws IOException {
        if (label == null) return;
        float x = num(label.get("x"));
        float yTop = num(label.get("y"));
        String text = String.valueOf(label.getOrDefault("text", ""));
        drawText(cs, text, PDType1Font.HELVETICA_BOLD, 12, x, pageH - yTop);
    }

    // ==================== helpers ====================
    private void drawText(PDPageContentStream cs, String text, PDType1Font font, int size, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private BufferedImage makeQr(String payload, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void drawCircle(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        float k = 0.552284749831f;
        float c = r * k;

        cs.moveTo(cx + r, cy);
        cs.curveTo(cx + r, cy + c, cx + c, cy + r, cx, cy + r);
        cs.curveTo(cx - c, cy + r, cx - r, cy + c, cx - r, cy);
        cs.curveTo(cx - r, cy - c, cx - c, cy - r, cx, cy - r);
        cs.curveTo(cx + c, cy - r, cx + r, cy - c, cx + r, cy);
        cs.closePath();
        cs.stroke();
    }

    private void drawRect(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void drawFilledSquare(PDPageContentStream cs, float x, float y, float size) throws IOException {
        cs.addRect(x, y, size, size);
        cs.fill();
    }

    private float num(Object o) {
        if (o == null) return 0f;
        if (o instanceof Number n) return n.floatValue();
        return Float.parseFloat(String.valueOf(o));
    }

    private String sanitize(String s) {
        return (s == null) ? "null" : s.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }
}
