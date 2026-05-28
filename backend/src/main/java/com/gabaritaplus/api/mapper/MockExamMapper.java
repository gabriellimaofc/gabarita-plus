package com.gabaritaplus.api.mapper;

import com.gabaritaplus.api.dto.mockexam.MockExamQuestionDetailResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamQuestionResponse;
import com.gabaritaplus.api.entity.MockExamAnswer;
import com.gabaritaplus.api.entity.MockExamQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockExamMapper {

    private final QuestionMapper questionMapper;

    public MockExamQuestionResponse toQuestionResponse(
            MockExamQuestion mockExamQuestion,
            MockExamAnswer answer
    ) {
        return new MockExamQuestionResponse(
                mockExamQuestion.getQuestion().getId(),
                mockExamQuestion.getQuestionOrder(),
                mockExamQuestion.getQuestion().getTitle(),
                mockExamQuestion.getQuestion().getSubject(),
                mockExamQuestion.getQuestion().getDifficulty(),
                answer != null,
                answer != null ? answer.isCorrect() : null
        );
    }

    public MockExamQuestionDetailResponse toQuestionDetailResponse(
            MockExamQuestion mockExamQuestion,
            MockExamAnswer answer,
            boolean revealAnswerKey
    ) {
        return new MockExamQuestionDetailResponse(
                mockExamQuestion.getQuestion().getId(),
                mockExamQuestion.getQuestionOrder(),
                mockExamQuestion.getQuestion().getTitle(),
                mockExamQuestion.getQuestion().getStatement(),
                mockExamQuestion.getQuestion().getStatementHtml(),
                mockExamQuestion.getQuestion().getImageUrl(),
                mockExamQuestion.getQuestion().getSubject(),
                mockExamQuestion.getQuestion().getTopic(),
                mockExamQuestion.getQuestion().getSubtopic(),
                mockExamQuestion.getQuestion().getDifficulty(),
                mockExamQuestion.getQuestion().getYear(),
                mockExamQuestion.getQuestion().getExam(),
                mockExamQuestion.getQuestion().getCompetency(),
                mockExamQuestion.getQuestion().getAbility(),
                answer != null ? answer.getChosenAlternative() : null,
                answer != null,
                answer != null ? answer.isCorrect() : null,
                revealAnswerKey ? mockExamQuestion.getQuestion().getCorrectAlternative() : null,
                revealAnswerKey ? mockExamQuestion.getQuestion().getExplanation() : null,
                mockExamQuestion.getQuestion().getAssets().stream().map(questionMapper::toAssetResponse).toList(),
                mockExamQuestion.getQuestion().getAlternatives().stream().map(questionMapper::toAlternativeResponse).toList()
        );
    }
}
