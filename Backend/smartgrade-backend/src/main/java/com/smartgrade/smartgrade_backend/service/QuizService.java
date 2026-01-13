package com.smartgrade.smartgrade_backend.service;

import com.smartgrade.smartgrade_backend.dto.quiz.*;
import com.smartgrade.smartgrade_backend.entity.*;
import com.smartgrade.smartgrade_backend.repository.ClassRepository;
import com.smartgrade.smartgrade_backend.repository.QuizRepository;
import com.smartgrade.smartgrade_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public QuizService(QuizRepository quizRepository, ClassRepository classRepository, UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    public List<QuizDto> findAll(String ownerEmail) {
        return quizRepository.findAllByOwner_EmailOrderByPkAsc(ownerEmail)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public QuizDto create(String ownerEmail, QuizDto dto) {
        if (dto == null) throw new RuntimeException("Body missing");
        if (dto.getId() == null || dto.getId().isBlank()) throw new RuntimeException("id is required");
        if (dto.getName() == null || dto.getName().isBlank()) throw new RuntimeException("name is required");
        if (dto.getDate() == null || dto.getDate().isBlank()) throw new RuntimeException("date is required");

        UserEntity owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        if (quizRepository.existsByIdAndOwner_Email(dto.getId(), ownerEmail)) {
            throw new RuntimeException("Quiz already exists");
        }

        QuizEntity quiz = new QuizEntity(dto.getId(), dto.getName(), dto.getDate(), owner);
        quiz.setClasses(resolveClasses(ownerEmail, dto.getClassIds()));

        // --- NEW: ext id config ---
        applyExtIdConfig(quiz, dto);

        // questions
        quiz.setQuestions(buildQuestions(quiz, dto.getQuestions()));

        QuizEntity saved = quizRepository.save(quiz);
        return toDto(saved);
    }

    public QuizDto update(String ownerEmail, String id, QuizDto dto) {
        QuizEntity existing = quizRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (dto.getName() != null && !dto.getName().isBlank()) existing.setName(dto.getName());
        if (dto.getDate() != null && !dto.getDate().isBlank()) existing.setDate(dto.getDate());

        if (dto.getClassIds() != null) {
            existing.setClasses(resolveClasses(ownerEmail, dto.getClassIds()));
        }

        // --- NEW: ext id config (allow update) ---
        if (dto.getExtIdMode() != null || dto.getExtIdLength() != null) {
            applyExtIdConfig(existing, dto);
            // optional: daca schimbi extId config, vei genera layout nou (version+1) la papers/generate
        }

        if (dto.getQuestions() != null) {
            // replace all (orphanRemoval=true)
            existing.getQuestions().clear();
            existing.getQuestions().addAll(buildQuestions(existing, dto.getQuestions()));
        }

        QuizEntity saved = quizRepository.save(existing);
        return toDto(saved);
    }

    public void delete(String ownerEmail, String id) {
        QuizEntity existing = quizRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        quizRepository.delete(existing);
    }

    private void applyExtIdConfig(QuizEntity quiz, QuizDto dto) {
        // mode
        String modeStr = (dto.getExtIdMode() == null || dto.getExtIdMode().isBlank())
                ? "DIGITS_ONLY"
                : dto.getExtIdMode().trim().toUpperCase(Locale.ROOT);

        ExtIdMode mode;
        try {
            mode = ExtIdMode.valueOf(modeStr);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid extIdMode. Allowed: DIGITS_ONLY, ALPHANUMERIC");
        }

        // length (cerinta ta: fix 10)
        Integer len = dto.getExtIdLength();
        if (len == null) len = 10;
        if (len != 10) throw new RuntimeException("extIdLength must be 10");

        quiz.setExtIdMode(mode);
        quiz.setExtIdLength(len);
    }

    private Set<ClassEntity> resolveClasses(String ownerEmail, List<String> classIds) {
        Set<ClassEntity> set = new HashSet<>();
        if (classIds == null) return set;

        for (String cid : classIds) {
            if (cid == null || cid.isBlank()) continue;
            ClassEntity cls = classRepository.findByIdAndOwner_Email(cid.trim(), ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Class not found: " + cid));
            set.add(cls);
        }
        return set;
    }

    private List<QuestionEntity> buildQuestions(QuizEntity quiz, List<QuestionDto> questionDtos) {
        List<QuestionEntity> out = new ArrayList<>();
        if (questionDtos == null) return out;

        int qi = 0;
        for (QuestionDto qd : questionDtos) {
            String qText = (qd.getText() == null) ? "" : qd.getText().trim();
            QuestionEntity q = new QuestionEntity(quiz, qText, qi++);
            q.setAnswers(buildAnswers(q, qd.getAnswers()));
            out.add(q);
        }
        return out;
    }

    private List<AnswerEntity> buildAnswers(QuestionEntity q, List<AnswerDto> answerDtos) {
        List<AnswerEntity> out = new ArrayList<>();
        if (answerDtos == null) return out;

        int ai = 0;
        for (AnswerDto ad : answerDtos) {
            String aText = (ad.getText() == null) ? "" : ad.getText().trim();
            out.add(new AnswerEntity(q, aText, ad.isCorrect(), ai++));
        }
        return out;
    }

    private QuizDto toDto(QuizEntity e) {
        List<String> classIds = e.getClasses().stream().map(ClassEntity::getId).sorted().toList();

        List<QuestionDto> questions = e.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuestionEntity::getPosition))
                .map(q -> new QuestionDto(
                        q.getText(),
                        q.getAnswers().stream()
                                .sorted(Comparator.comparingInt(AnswerEntity::getPosition))
                                .map(a -> new AnswerDto(a.getText(), a.isCorrect()))
                                .toList()
                ))
                .toList();

        return new QuizDto(
                e.getId(),
                e.getName(),
                e.getDate(),
                classIds,
                questions,
                e.getExtIdMode() == null ? null : e.getExtIdMode().name(),
                e.getExtIdLength(),
                e.getLayoutCurrentVersion()
        );
    }
}
