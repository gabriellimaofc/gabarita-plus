package com.gabaritaplus.api.dto.mockexam;

public record MockExamQuestionResponse(
        Long questionId,
        Integer questionOrder,
        String title,
        String subject,
        String difficulty
) {
}
