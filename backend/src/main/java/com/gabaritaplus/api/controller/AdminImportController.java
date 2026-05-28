package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.importer.ImportBatchResponse;
import com.gabaritaplus.api.dto.importer.ImportQuestionsPayload;
import com.gabaritaplus.api.dto.importer.ImportReportResponse;
import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewDetailResponse;
import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewSummaryResponse;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.service.QuestionService;
import com.gabaritaplus.api.service.importer.QuestionImportService;
import com.gabaritaplus.api.util.PageUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Importacao Admin")
@RestController
@RequestMapping("/admin/import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminImportController {

    private final QuestionImportService questionImportService;
    private final QuestionService questionService;

    @PostMapping(value = "/questions/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportReportResponse>> importJson(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "Importacao JSON processada com sucesso.",
                questionImportService.importJson(file)
        ));
    }

    @PostMapping(value = "/questions/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportReportResponse>> importCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "Importacao CSV processada com sucesso.",
                questionImportService.importCsv(file)
        ));
    }

    @PostMapping("/questions/dry-run")
    public ResponseEntity<ApiResponse<ImportReportResponse>> dryRun(@Valid @RequestBody ImportQuestionsPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dry-run concluido com sucesso.",
                questionImportService.dryRun(payload)
        ));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> listBatches() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lotes de importacao carregados com sucesso.",
                questionImportService.listBatches()
        ));
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> getBatch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lote de importacao carregado com sucesso.",
                questionImportService.getBatch(id)
        ));
    }

    @GetMapping("/questions/review")
    public ResponseEntity<ApiResponse<List<AdminImportedQuestionReviewSummaryResponse>>> listReviewQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) List<QuestionImportStatus> status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer year
    ) {
        Page<AdminImportedQuestionReviewSummaryResponse> result = questionService.listReviewQuestions(
                status,
                source,
                year,
                PageRequest.of(page, size, Sort.by(direction, sortBy))
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Questoes em revisao carregadas com sucesso.",
                result.getContent(),
                PageUtils.metadata(result)
        ));
    }

    @GetMapping("/questions/review/{id}")
    public ResponseEntity<ApiResponse<AdminImportedQuestionReviewDetailResponse>> getReviewQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Questao em revisao carregada com sucesso.",
                questionService.getReviewQuestion(id)
        ));
    }
}
