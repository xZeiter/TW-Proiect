package com.smartgrade.smartgrade_backend.controller;

import com.smartgrade.smartgrade_backend.config.CurrentUser;
import com.smartgrade.smartgrade_backend.dto.layout.LayoutResponse;
import com.smartgrade.smartgrade_backend.dto.papers.GeneratePapersRequest;
import com.smartgrade.smartgrade_backend.dto.papers.GeneratePapersResponse;
import com.smartgrade.smartgrade_backend.entity.QuestionEntity;
import com.smartgrade.smartgrade_backend.entity.QuizEntity;
import com.smartgrade.smartgrade_backend.entity.QuizLayoutEntity;
import com.smartgrade.smartgrade_backend.entity.SheetEntity;
import com.smartgrade.smartgrade_backend.repository.QuestionRepository;
import com.smartgrade.smartgrade_backend.repository.QuizRepository;
import com.smartgrade.smartgrade_backend.service.LayoutService;
import com.smartgrade.smartgrade_backend.service.PaperPdfService;
import com.smartgrade.smartgrade_backend.service.SheetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class PaperController {

    private final CurrentUser currentUser;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    private final LayoutService layoutService;
    private final SheetService sheetService;
    private final PaperPdfService paperPdfService;

    public PaperController(
            CurrentUser currentUser,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            LayoutService layoutService,
            SheetService sheetService,
            PaperPdfService paperPdfService
    ) {
        this.currentUser = currentUser;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.layoutService = layoutService;
        this.sheetService = sheetService;
        this.paperPdfService = paperPdfService;
    }

    @PostMapping("/{quizId}/papers/generate")
    public GeneratePapersResponse generate(
            HttpServletRequest request,
            @PathVariable String quizId,
            @RequestBody(required = false) GeneratePapersRequest body
    ) {
        String email = currentUser.email(request);

        // 1) quiz simplu (fara fetch pe colectii)
        QuizEntity quiz = quizRepository.findByIdAndOwner_Email(quizId, email)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // 2) questions + answers separat (EntityGraph doar pe answers)
      List<QuestionEntity> questions = questionRepository.findAllByQuiz_PkOrderByPositionAsc(quiz.getPk());

       quiz.getQuestions().clear();
       quiz.getQuestions().addAll(questions);


        // layout versioning + persist
        QuizLayoutEntity layout = layoutService.buildAndPersistLayoutIfNeeded(quiz);
        int layoutVersion = layout.getVersion();

        // create sheets
        int count = (body == null || body.getCount() == null) ? 1 : body.getCount();
        List<SheetEntity> sheets = sheetService.createSheets(quiz, layoutVersion, count);

        // generate PDF on disk
        Path pdfPath = paperPdfService.generateAndStorePdf(email, quiz, layoutVersion, layout.getLayoutJson(), sheets);

        List<Long> sheetIds = sheets.stream().map(SheetEntity::getId).toList();
        String downloadUrl = "/api/quizzes/" + quizId + "/papers/download?layoutVersion=" + layoutVersion;

        return new GeneratePapersResponse(quizId, layoutVersion, sheetIds, downloadUrl);
    }

    @GetMapping("/{quizId}/layout")
public LayoutResponse getLayout(HttpServletRequest request,
                               @PathVariable String quizId,
                               @RequestParam(required = false) Integer version) {
    String email = currentUser.email(request);

    QuizEntity quiz = quizRepository.findByIdAndOwner_Email(quizId, email)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));

    QuizLayoutEntity layout = layoutService.getLayoutOrThrow(quiz.getPk(), version);
    return new LayoutResponse(quizId, layout.getVersion(), layout.getLayoutJson());
}


    @GetMapping("/{quizId}/papers/download")
    public ResponseEntity<Resource> download(HttpServletRequest request,
                                            @PathVariable String quizId,
                                            @RequestParam Integer layoutVersion) {
        String email = currentUser.email(request);

        quizRepository.findByIdAndOwner_Email(quizId, email)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Path path = Path.of("storage", "papers",
                email.replaceAll("[^a-zA-Z0-9._-]+", "_"),
                quizId.replaceAll("[^a-zA-Z0-9._-]+", "_"),
                String.valueOf(layoutVersion),
                "papers.pdf"
        );

        Resource resource = new FileSystemResource(path.toFile());
        if (!resource.exists()) throw new RuntimeException("PDF not found");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"papers-" + quizId + "-v" + layoutVersion + ".pdf\"")
                .body(resource);
    }
}
