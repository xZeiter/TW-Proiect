package com.smartgrade.smartgrade_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrade.smartgrade_backend.entity.*;
import com.smartgrade.smartgrade_backend.repository.QuizLayoutRepository;
import com.smartgrade.smartgrade_backend.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class LayoutService {

    private final QuizLayoutRepository layoutRepository;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper;

    public LayoutService(QuizLayoutRepository layoutRepository,
                         QuizRepository quizRepository,
                         ObjectMapper objectMapper) {
        this.layoutRepository = layoutRepository;
        this.quizRepository = quizRepository;
        this.objectMapper = objectMapper;
    }

    // ✅ folosit de controller /layout: lucreaza pe quizPk ca sa nu mai ai ambiguitati
    public QuizLayoutEntity getLayoutOrThrow(Long quizPk, Integer version) {
        if (version == null) {
            return layoutRepository.findTopByQuiz_PkOrderByVersionDesc(quizPk)
                    .orElseThrow(() -> new RuntimeException("Layout not found for quizPk " + quizPk));
        }

        return layoutRepository.findByQuiz_PkAndVersion(quizPk, version)
                .orElseThrow(() -> new RuntimeException("Layout not found for quizPk " + quizPk + " version " + version));
    }

    // ✅ IMPORTANT: reuse pe hash + persist layoutCurrentVersion in DB
    @Transactional
    public QuizLayoutEntity buildAndPersistLayoutIfNeeded(QuizEntity quiz) {
        if (quiz.getPk() == null) throw new RuntimeException("Quiz pk missing");

        String layoutHash = computeLayoutHashStrong(quiz);

        // 1) Reuse layout daca hash identic
        Optional<QuizLayoutEntity> existingSame =
                layoutRepository.findByQuiz_PkAndLayoutHash(quiz.getPk(), layoutHash);

        if (existingSame.isPresent()) {
            QuizLayoutEntity e = existingSame.get();
            quiz.setLayoutCurrentVersion(e.getVersion());
            quizRepository.save(quiz);
            return e;
        }

        // 2) Altfel, versiune noua
        int nextVersion = layoutRepository.findTopByQuiz_PkOrderByVersionDesc(quiz.getPk())
                .map(x -> x.getVersion() + 1)
                .orElse(1);

        String layoutJson = buildLayoutJson(quiz, nextVersion);

        QuizLayoutEntity created = new QuizLayoutEntity(quiz, nextVersion, layoutJson);
        created.setLayoutHash(layoutHash);

        QuizLayoutEntity saved = layoutRepository.save(created);

        quiz.setLayoutCurrentVersion(saved.getVersion());
        quizRepository.save(quiz);

        return saved;
    }

    // === layout builder (aproape identic cu al tau; qIndex e deja consecutiv) ===
    public String buildLayoutJson(QuizEntity quiz, int version) {
        int pageW = 595;
        int pageH = 842;

        int r = 6;

        int left = 50;
        int top = 60;

        List<Map<String, Object>> anchors = List.of(
                anchor("TL", 22, 22, 14),
                anchor("TR", pageW - 22, 22, 14),
                anchor("BL", 22, pageH - 22, 14),
                anchor("BR", pageW - 22, pageH - 22, 14)
        );

        Map<String, Object> qr = Map.of("x", pageW - 135, "y", 40, "size", 92);

        Map<String, Object> labels = new LinkedHashMap<>();

        int extLabelX = left;
        int extLabelY = top + 28;
        int extGridTopY = extLabelY + 26;

        labels.put("extId", Map.of("x", extLabelX, "y", extLabelY, "text", "EXT ID (bule):"));

        Map<String, Object> extId = buildExtIdLayout(quiz, left, extGridTopY, r);

        int nameLabelX = left;
        int nameLabelY = extGridTopY + extIdBoxH(extId) + 24;
        labels.put("name", Map.of("x", nameLabelX, "y", nameLabelY, "text", "NAME (scris):"));

        int nameBoxX = left;
        int nameBoxY = nameLabelY + 18;
        Map<String, Object> name = Map.of(
                "box", Map.of("x", nameBoxX, "y", nameBoxY, "w", 420, "h", 56)
        );

        int questionsLabelX = left;
        int qStartY = nameBoxY + 56 + 40;
        labels.put("questions", Map.of("x", questionsLabelX, "y", qStartY - 18, "text", "Raspunsuri:"));

        int rowH = 28;

        List<Map<String, Object>> questions = new ArrayList<>();
        List<QuestionEntity> qs = quiz.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuestionEntity::getPosition).thenComparing(QuestionEntity::getPk))
                .toList();

        for (int qIndex = 0; qIndex < qs.size(); qIndex++) {
            QuestionEntity q = qs.get(qIndex);

            int y = qStartY + qIndex * rowH;

            // IMPORTANT: daca answers nu sunt fetchuite, optionCount va fi 0 => intrebarea dispare in PDF
            int optionCount = (q.getAnswers() == null) ? 0 : q.getAnswers().size();

            List<Map<String, Object>> options = new ArrayList<>();
            int x0 = left + 70;
            int dx = 28;

            for (int i = 0; i < optionCount; i++) {
                String label = String.valueOf((char) ('A' + i));
                options.add(Map.of(
                        "label", label,
                        "x", x0 + i * dx,
                        "y", y,
                        "r", r
                ));
            }

            questions.add(Map.of(
                    "qIndex", qIndex,
                    "questionPk", q.getPk(),
                    "rowBox", Map.of("x", left, "y", y - 14, "w", pageW - left * 2, "h", rowH),
                    "options", options
            ));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("quizId", quiz.getId());
        root.put("version", version);
        root.put("page", Map.of("w", pageW, "h", pageH, "unit", "pt"));
        root.put("anchors", anchors);
        root.put("qr", qr);
        root.put("labels", labels);
        root.put("extId", extId);
        root.put("name", name);
        root.put("questions", questions);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize layout JSON", e);
        }
    }

    private int extIdBoxH(Map<String, Object> extId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> box = (Map<String, Object>) extId.get("box");
            Object h = box.get("h");
            if (h instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(h));
        } catch (Exception ignored) {
            return 200;
        }
    }

    private Map<String, Object> buildExtIdLayout(QuizEntity quiz, int x, int y, int r) {
        int length = quiz.getExtIdLength() == null ? 10 : quiz.getExtIdLength();
        ExtIdMode mode = quiz.getExtIdMode() == null ? ExtIdMode.DIGITS_ONLY : quiz.getExtIdMode();

        List<String> symbols;
        if (mode == ExtIdMode.DIGITS_ONLY) {
            symbols = List.of("0","1","2","3","4","5","6","7","8","9");
        } else {
            List<String> tmp = new ArrayList<>();
            for (char c = '0'; c <= '9'; c++) tmp.add(String.valueOf(c));
            for (char c = 'A'; c <= 'Z'; c++) tmp.add(String.valueOf(c));
            symbols = tmp;
        }

        int colW = 24;
        int rowH = 16;

        List<Map<String, Object>> cells = new ArrayList<>();
        for (int pos = 0; pos < length; pos++) {
            int cx = x + pos * colW;
            List<Map<String, Object>> bubbles = new ArrayList<>();
            for (int si = 0; si < symbols.size(); si++) {
                int by = y + si * rowH;
                bubbles.add(Map.of("sym", symbols.get(si), "x", cx, "y", by, "r", r));
            }
            cells.add(Map.of("pos", pos, "symbols", symbols, "bubbles", bubbles));
        }

        return Map.of(
                "mode", mode.name(),
                "length", length,
                "box", Map.of("x", x - 10, "y", y - 10, "w", length * colW + 20, "h", symbols.size() * rowH + 20),
                "cells", cells
        );
    }

    private Map<String, Object> anchor(String id, int x, int y, int size) {
        return Map.of("id", id, "x", x, "y", y, "size", size);
    }

    // ✅ hash puternic: include text + answers + correct + ordine
    private String computeLayoutHashStrong(QuizEntity quiz) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            md.update(("quiz=" + safe(quiz.getId())).getBytes(StandardCharsets.UTF_8));
            md.update(("|extMode=" + String.valueOf(quiz.getExtIdMode())).getBytes(StandardCharsets.UTF_8));
            md.update(("|extLen=" + String.valueOf(quiz.getExtIdLength())).getBytes(StandardCharsets.UTF_8));

            List<QuestionEntity> qs = quiz.getQuestions().stream()
                    .sorted(Comparator.comparingInt(QuestionEntity::getPosition).thenComparing(QuestionEntity::getPk))
                    .toList();

            for (QuestionEntity q : qs) {
                md.update(("\nQ|" + q.getPk() + "|" + q.getPosition() + "|" + safe(q.getText()))
                        .getBytes(StandardCharsets.UTF_8));

                List<AnswerEntity> ans = (q.getAnswers() == null) ? List.of()
                        : q.getAnswers().stream()
                        .sorted(Comparator.comparingInt(AnswerEntity::getPosition).thenComparing(AnswerEntity::getPk))
                        .toList();

                for (AnswerEntity a : ans) {
                    md.update(("\nA|" + a.getPk() + "|" + a.getPosition() + "|" + safe(a.getText()) + "|" + a.isCorrect())
                            .getBytes(StandardCharsets.UTF_8));
                }
            }

            return toHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute layout hash", e);
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
