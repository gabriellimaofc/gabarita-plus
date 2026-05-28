package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.importer.ImportReportResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevImportRequest;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevPreviewResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevYearResponse;
import com.gabaritaplus.api.service.importer.EnemDevImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Importacao ENEM Dev Admin")
@RestController
@RequestMapping("/admin/import/enem-dev")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEnemDevImportController {

    private final EnemDevImportService enemDevImportService;

    @GetMapping("/years")
    public ResponseEntity<ApiResponse<List<EnemDevYearResponse>>> listYears() {
        return ResponseEntity.ok(ApiResponse.success(
                "Anos disponiveis na fonte auxiliar carregados com sucesso.",
                enemDevImportService.listYears()
        ));
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<EnemDevPreviewResponse>> preview(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String language
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Preview do lote do enem.dev gerado com sucesso.",
                enemDevImportService.preview(new EnemDevImportRequest(year, limit, offset, language))
        ));
    }

    @PostMapping("/dry-run")
    public ResponseEntity<ApiResponse<ImportReportResponse>> dryRun(@Valid @RequestBody EnemDevImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dry-run do enem.dev concluido com sucesso.",
                enemDevImportService.dryRun(request)
        ));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ImportReportResponse>> importQuestions(@Valid @RequestBody EnemDevImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Importacao do enem.dev processada com sucesso.",
                enemDevImportService.importQuestions(request)
        ));
    }
}
