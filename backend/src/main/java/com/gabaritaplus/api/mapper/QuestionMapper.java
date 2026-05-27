package com.gabaritaplus.api.mapper;

import com.gabaritaplus.api.dto.question.AlternativeRequest;
import com.gabaritaplus.api.dto.question.AlternativeResponse;
import com.gabaritaplus.api.dto.question.ErrorNotebookResponse;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.dto.question.QuestionResponse;
import com.gabaritaplus.api.dto.question.UserAnswerResponse;
import com.gabaritaplus.api.entity.Alternative;
import com.gabaritaplus.api.entity.ErrorNotebook;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.UserAnswer;
import com.gabaritaplus.api.entity.enums.ReviewPriority;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class QuestionMapper {

    public Question toEntity(QuestionRequest request) {
        Question question = new Question();
        question.setTitle(request.title());
        question.setStatement(request.statement());
        question.setImageUrl(request.imageUrl());
        question.setSubject(request.subject());
        question.setTopic(request.topic());
        question.setSubtopic(request.subtopic());
        question.setDifficulty(request.difficulty());
        question.setYear(request.year());
        question.setExam(request.exam());
        question.setCompetency(request.competency());
        question.setAbility(request.ability());
        question.setExplanation(request.explanation());
        question.setCorrectAlternative(request.correctAlternative());
        request.alternatives().forEach(item -> {
            Alternative alternative = toAlternative(item);
            alternative.setQuestion(question);
            question.getAlternatives().add(alternative);
        });
        return question;
    }

    public Alternative toAlternative(AlternativeRequest request) {
        Alternative alternative = new Alternative();
        alternative.setLetter(request.letter());
        alternative.setText(request.text());
        alternative.setCorrect(request.correct());
        return alternative;
    }

    public AlternativeResponse toAlternativeResponse(Alternative alternative) {
        return new AlternativeResponse(
                alternative.getId(),
                alternative.getLetter(),
                alternative.getText()
        );
    }

    public QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getStatement(),
                question.getImageUrl(),
                question.getSubject(),
                question.getTopic(),
                question.getSubtopic(),
                question.getDifficulty(),
                question.getYear(),
                question.getExam(),
                question.getCompetency(),
                question.getAbility(),
                question.getExplanation(),
                question.getCorrectAlternative(),
                false,
                null,
                null,
                question.getAlternatives().stream().map(this::toAlternativeResponse).toList(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    public UserAnswerResponse toAnswerResponse(UserAnswer answer) {
        return new UserAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getChosenAlternative(),
                answer.isCorrect(),
                answer.getQuestion().getCorrectAlternative(),
                answer.getQuestion().getExplanation(),
                answer.getAttemptNumber(),
                answer.getTimeSpentSeconds(),
                answer.getCreatedAt()
        );
    }

    public ErrorNotebookResponse toErrorNotebookResponse(
            ErrorNotebook notebook,
            LocalDate lastErrorAt,
            ReviewPriority priority
    ) {
        return new ErrorNotebookResponse(
                notebook.getId(),
                notebook.getQuestion().getId(),
                notebook.getQuestion().getTitle(),
                notebook.getQuestion().getSubject(),
                notebook.getQuestion().getTopic(),
                notebook.getQuestion().getDifficulty(),
                notebook.getErrorCount(),
                lastErrorAt,
                notebook.getLastReviewedAt(),
                notebook.getNextReviewAt(),
                notebook.getMasteryStatus(),
                priority,
                notebook.getUpdatedAt()
        );
    }

    public List<QuestionResponse> toResponses(List<Question> questions) {
        return questions.stream().map(this::toResponse).toList();
    }
}
