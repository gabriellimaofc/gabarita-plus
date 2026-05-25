package com.gabaritaplus.api.mapper;

import com.gabaritaplus.api.dto.mockexam.MockExamQuestionResponse;
import com.gabaritaplus.api.entity.MockExamQuestion;
import org.springframework.stereotype.Component;

@Component
public class MockExamMapper {

    public MockExamQuestionResponse toQuestionResponse(MockExamQuestion mockExamQuestion) {
        return new MockExamQuestionResponse(
                mockExamQuestion.getQuestion().getId(),
                mockExamQuestion.getQuestionOrder(),
                mockExamQuestion.getQuestion().getTitle(),
                mockExamQuestion.getQuestion().getSubject(),
                mockExamQuestion.getQuestion().getDifficulty().name()
        );
    }
}
