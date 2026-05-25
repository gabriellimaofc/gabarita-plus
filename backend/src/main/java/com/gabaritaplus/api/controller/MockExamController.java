package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.mockexam.FinishMockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamResponse;
import com.gabaritaplus.api.service.MockExamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Simulados")
@RestController
@RequestMapping("/mock-exams")
@RequiredArgsConstructor
public class MockExamController {

    private final MockExamService mockExamService;

    @PostMapping
    public ResponseEntity<ApiResponse<MockExamResponse>> create(@Valid @RequestBody MockExamRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Simulado criado com sucesso.", mockExamService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MockExamResponse>>> listMine() {
        return ResponseEntity.ok(ApiResponse.success("Simulados carregados com sucesso.", mockExamService.listMine()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MockExamResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Simulado carregado com sucesso.", mockExamService.getById(id)));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<ApiResponse<MockExamResponse>> finish(@PathVariable Long id,
                                                                @Valid @RequestBody FinishMockExamRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Simulado finalizado com sucesso.", mockExamService.finish(id, request)));
    }
}
