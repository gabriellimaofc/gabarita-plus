package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.question.ErrorNotebookFilterRequest;
import com.gabaritaplus.api.dto.question.ErrorNotebookResponse;
import com.gabaritaplus.api.dto.question.QuestionFilterRequest;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.dto.question.QuestionResponse;
import com.gabaritaplus.api.dto.question.UpdateErrorNotebookStatusRequest;
import com.gabaritaplus.api.dto.question.UserAnswerRequest;
import com.gabaritaplus.api.dto.question.UserAnswerResponse;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.MasteryStatus;
import com.gabaritaplus.api.entity.enums.ReviewPriority;
import com.gabaritaplus.api.service.QuestionService;
import com.gabaritaplus.api.util.PageUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Questoes")
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String subtopic,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String exam,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) Boolean incorrectOnly,
            @RequestParam(required = false) Boolean favoritesOnly
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        QuestionFilterRequest filter = new QuestionFilterRequest(
                search,
                subject,
                topic,
                subtopic,
                difficulty,
                year,
                exam,
                answered,
                incorrectOnly,
                favoritesOnly
        );
        Page<QuestionResponse> result = questionService.search(filter, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Questoes carregadas com sucesso.", result.getContent(), PageUtils.metadata(result))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Questao carregada com sucesso.", questionService.getById(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Questao criada com sucesso.", questionService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Questao atualizada com sucesso.", questionService.update(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Questao removida com sucesso."));
    }

    @PostMapping("/answers")
    public ResponseEntity<ApiResponse<UserAnswerResponse>> answer(@Valid @RequestBody UserAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Resposta registrada com sucesso.", questionService.answerQuestion(request)));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<QuestionResponse>> toggleFavorite(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Favorito atualizado com sucesso.", questionService.toggleFavorite(id)));
    }

    @GetMapping("/error-notebook")
    public ResponseEntity<ApiResponse<List<ErrorNotebookResponse>>> errorNotebook(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) MasteryStatus masteryStatus,
            @RequestParam(required = false) ReviewPriority priority
    ) {
        ErrorNotebookFilterRequest filter = new ErrorNotebookFilterRequest(
                subject,
                topic,
                difficulty,
                masteryStatus,
                priority
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Caderno de erros carregado com sucesso.",
                questionService.getErrorNotebook(filter)
        ));
    }

    @PatchMapping("/error-notebook/{questionId}")
    public ResponseEntity<ApiResponse<ErrorNotebookResponse>> updateErrorNotebookStatus(
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateErrorNotebookStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Status do caderno de erros atualizado com sucesso.",
                questionService.updateErrorNotebookStatus(questionId, request)
        ));
    }
}
