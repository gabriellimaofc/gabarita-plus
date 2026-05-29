package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.importer.ImportBatchResponse;
import com.gabaritaplus.api.dto.importer.ImportQuestionsPayload;
import com.gabaritaplus.api.dto.importer.ImportReportResponse;
import com.gabaritaplus.api.dto.importer.official.OfficialExamSourceRequest;
import com.gabaritaplus.api.dto.importer.official.OfficialExamSourceResponse;
import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewDetailResponse;
import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewSummaryResponse;
import com.gabaritaplus.api.dto.importer.review.AutoValidationBatchResponse;
import com.gabaritaplus.api.dto.importer.review.AutoValidationCountersResponse;
import com.gabaritaplus.api.dto.importer.review.OfficialValidationReportResponse;
import com.gabaritaplus.api.dto.importer.review.UpdateImportedQuestionStatusRequest;
import com.gabaritaplus.api.dto.importer.review.ValidateOfficialSourceRequest;
import com.gabaritaplus.api.entity.enums.AutoValidationStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.service.QuestionService;
import com.gabaritaplus.api.service.importer.QuestionAutoValidationService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final QuestionAutoValidationService questionAutoValidationService;

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
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) AutoValidationStatus autoValidationStatus
    ) {
        Page<AdminImportedQuestionReviewSummaryResponse> result = questionService.listReviewQuestions(
                status,
                source,
                year,
                subject,
                autoValidationStatus,
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

    @PatchMapping("/questions/review/{id}/status")
    public ResponseEntity<ApiResponse<AdminImportedQuestionReviewDetailResponse>> updateReviewQuestionStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateImportedQuestionStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Status da questao em revisao atualizado com sucesso.",
                questionService.updateReviewQuestionStatus(id, request)
        ));
    }

    @PatchMapping("/questions/review/{id}/validate-official-source")
    public ResponseEntity<ApiResponse<AdminImportedQuestionReviewDetailResponse>> validateOfficialSource(
            @PathVariable Long id,
            @RequestBody ValidateOfficialSourceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Questao marcada como validada contra a fonte oficial.",
                questionService.validateOfficialSource(id, request)
        ));
    }

    @PostMapping("/questions/review/{id}/publish")
    public ResponseEntity<ApiResponse<AdminImportedQuestionReviewDetailResponse>> publishReviewQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Questao publicada com sucesso.",
                questionService.publishReviewQuestion(id)
        ));
    }

    @PostMapping("/official-sources")
    public ResponseEntity<ApiResponse<OfficialExamSourceResponse>> createOfficialSource(
            @Valid @RequestBody OfficialExamSourceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Fonte oficial cadastrada com sucesso.",
                questionAutoValidationService.createOfficialSource(request)
        ));
    }

    @GetMapping("/official-sources")
    public ResponseEntity<ApiResponse<List<OfficialExamSourceResponse>>> listOfficialSources() {
        return ResponseEntity.ok(ApiResponse.success(
                "Fontes oficiais carregadas com sucesso.",
                questionAutoValidationService.listOfficialSources()
        ));
    }

    @PostMapping("/questions/{id}/auto-validate")
    public ResponseEntity<ApiResponse<AdminImportedQuestionReviewDetailResponse>> autoValidateQuestion(@PathVariable Long id) {
        questionAutoValidationService.autoValidate(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Auto validacao concluida com sucesso.",
                questionService.getReviewQuestion(id)
        ));
    }

    @PostMapping("/questions/auto-validate-batch")
    public ResponseEntity<ApiResponse<AutoValidationBatchResponse>> autoValidateBatch() {
        return ResponseEntity.ok(ApiResponse.success(
                "Auto validacao em lote concluida com sucesso.",
                questionAutoValidationService.autoValidateBatch()
        ));
    }

    @PostMapping("/questions/auto-publish-safe")
    public ResponseEntity<ApiResponse<AutoValidationBatchResponse>> autoPublishSafe() {
        return ResponseEntity.ok(ApiResponse.success(
                "Publicacao automatica segura processada com sucesso.",
                questionAutoValidationService.autoPublishSafe()
        ));
    }

    @PostMapping("/questions/{id}/recover-assets")
    public ResponseEntity<ApiResponse<OfficialValidationReportResponse>> recoverAssets(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Recuperacao de assets contra fonte oficial processada com seguranca.",
                questionAutoValidationService.recoverAssets(id)
        ));
    }

    @PostMapping("/questions/recover-assets-batch")
    public ResponseEntity<ApiResponse<OfficialValidationReportResponse>> recoverAssetsBatch() {
        return ResponseEntity.ok(ApiResponse.success(
                "Recuperacao de assets em lote processada com seguranca.",
                questionAutoValidationService.recoverAssetsBatch()
        ));
    }

    @PostMapping("/questions/{id}/validate-against-official-source")
    public ResponseEntity<ApiResponse<OfficialValidationReportResponse>> validateAgainstOfficialSource(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Validacao automatica contra fonte oficial processada com seguranca.",
                questionAutoValidationService.validateAgainstOfficialSource(id)
        ));
    }

    @PostMapping("/questions/validate-against-official-source-batch")
    public ResponseEntity<ApiResponse<OfficialValidationReportResponse>> validateAgainstOfficialSourceBatch() {
        return ResponseEntity.ok(ApiResponse.success(
                "Validacao automatica em lote contra fonte oficial processada com seguranca.",
                questionAutoValidationService.validateAgainstOfficialSourceBatch()
        ));
    }

    @GetMapping("/questions/review/counters")
    public ResponseEntity<ApiResponse<AutoValidationCountersResponse>> reviewCounters() {
        return ResponseEntity.ok(ApiResponse.success(
                "Contadores de revisao carregados com sucesso.",
                questionAutoValidationService.counters()
        ));
    }
}
