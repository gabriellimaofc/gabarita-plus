package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.mockexam.FinishMockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamAnswerRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamAnswerResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamQuestionDetailResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamResultResponse;
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

    @GetMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<List<MockExamQuestionDetailResponse>>> getQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Questoes do simulado carregadas com sucesso.",
                mockExamService.getQuestions(id)
        ));
    }

    @PostMapping("/{id}/answers")
    public ResponseEntity<ApiResponse<MockExamAnswerResponse>> answer(
            @PathVariable Long id,
            @Valid @RequestBody MockExamAnswerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Resposta do simulado registrada com sucesso.",
                mockExamService.answerQuestion(id, request)
        ));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<ApiResponse<MockExamResultResponse>> finish(
            @PathVariable Long id,
            @Valid @RequestBody FinishMockExamRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Simulado finalizado com sucesso.",
                mockExamService.finish(id, request)
        ));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<MockExamResultResponse>> getResult(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Resultado do simulado carregado com sucesso.",
                mockExamService.getResult(id)
        ));
    }
}
