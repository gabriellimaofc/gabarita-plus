package com.gabaritaplus.api.mapper;

import com.gabaritaplus.api.dto.question.AlternativeRequest;
import com.gabaritaplus.api.dto.question.AlternativeResponse;
import com.gabaritaplus.api.dto.question.ErrorNotebookResponse;
import com.gabaritaplus.api.dto.question.QuestionAssetRequest;
import com.gabaritaplus.api.dto.question.QuestionAssetResponse;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.dto.question.QuestionResponse;
import com.gabaritaplus.api.dto.question.UserAnswerResponse;
import com.gabaritaplus.api.entity.Alternative;
import com.gabaritaplus.api.entity.ErrorNotebook;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.QuestionAsset;
import com.gabaritaplus.api.entity.UserAnswer;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.entity.enums.ReviewPriority;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class QuestionMapper {

    public Question toEntity(QuestionRequest request) {
        Question question = new Question();
        apply(question, request);
        return question;
    }

    public void apply(Question question, QuestionRequest request) {
        question.setTitle(request.title());
        question.setStatement(request.statement());
        question.setStatementHtml(request.statementHtml());
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
        question.setSource(request.source());
        question.setSourceUrl(request.sourceUrl());
        question.setSourceExam(request.sourceExam());
        question.setSourceYear(request.sourceYear());
        question.setSourceQuestionNumber(request.sourceQuestionNumber());
        question.setSourceBookColor(request.sourceBookColor());
        question.setSourceDay(request.sourceDay());
        question.setSourcePage(request.sourcePage());
        question.setImportStatus(request.importStatus() == null ? QuestionImportStatus.PUBLISHED : request.importStatus());
        question.setImportedAt(question.getImportedAt() == null ? OffsetDateTime.now() : question.getImportedAt());

        question.getAlternatives().clear();
        request.alternatives().forEach(item -> {
            Alternative alternative = toAlternative(item);
            alternative.setQuestion(question);
            question.getAlternatives().add(alternative);
        });

        question.getAssets().clear();
        if (request.assets() != null) {
            request.assets().forEach(item -> {
                QuestionAsset asset = toAsset(item);
                asset.setQuestion(question);
                question.getAssets().add(asset);
            });
        }

        attachAlternativeAssets(question, request.alternatives());
    }

    public Alternative toAlternative(AlternativeRequest request) {
        Alternative alternative = new Alternative();
        alternative.setLetter(request.letter());
        alternative.setText(request.text());
        alternative.setHtml(request.html());
        alternative.setCorrect(request.correct());
        return alternative;
    }

    public QuestionAsset toAsset(QuestionAssetRequest request) {
        QuestionAsset asset = new QuestionAsset();
        asset.setType(request.type());
        asset.setUrl(request.url());
        asset.setStoragePath(request.storagePath());
        asset.setOriginalFileName(request.originalFileName());
        asset.setSourcePage(request.sourcePage());
        asset.setCropX(request.cropX());
        asset.setCropY(request.cropY());
        asset.setCropWidth(request.cropWidth());
        asset.setCropHeight(request.cropHeight());
        asset.setAltText(request.altText());
        asset.setCaption(request.caption());
        asset.setChecksum(request.checksum());
        return asset;
    }

    public AlternativeResponse toAlternativeResponse(Alternative alternative) {
        return new AlternativeResponse(
                alternative.getId(),
                alternative.getLetter(),
                alternative.getText(),
                alternative.getHtml(),
                alternative.getAssets().stream().map(this::toAssetResponse).toList()
        );
    }

    public QuestionAssetResponse toAssetResponse(QuestionAsset asset) {
        return new QuestionAssetResponse(
                asset.getId(),
                asset.getQuestion() == null ? null : asset.getQuestion().getId(),
                asset.getAlternative() == null ? null : asset.getAlternative().getId(),
                asset.getType(),
                asset.getUrl(),
                asset.getStoragePath(),
                asset.getOriginalFileName(),
                asset.getSourcePage(),
                asset.getCropX(),
                asset.getCropY(),
                asset.getCropWidth(),
                asset.getCropHeight(),
                asset.getAltText(),
                asset.getCaption(),
                asset.getChecksum()
        );
    }

    public QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getStatement(),
                question.getStatementHtml(),
                question.getImageUrl(),
                question.getSubject(),
                question.getTopic(),
                question.getSubtopic(),
                question.getDifficulty(),
                question.getYear(),
                question.getExam(),
                question.getCompetency(),
                question.getAbility(),
                question.getSource(),
                question.getSourceUrl(),
                question.getSourceExam(),
                question.getSourceYear(),
                question.getSourceQuestionNumber(),
                question.getSourceBookColor(),
                question.getSourceDay(),
                question.getSourcePage(),
                question.getStatementHash(),
                question.getImportStatus(),
                question.getExplanation(),
                question.getCorrectAlternative(),
                false,
                null,
                null,
                question.getAssets().stream().map(this::toAssetResponse).toList(),
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

    private void attachAlternativeAssets(Question question, List<AlternativeRequest> alternativeRequests) {
        if (alternativeRequests == null) {
            return;
        }

        for (int index = 0; index < alternativeRequests.size() && index < question.getAlternatives().size(); index++) {
            AlternativeRequest request = alternativeRequests.get(index);
            Alternative alternative = question.getAlternatives().get(index);
            if (request.assets() == null) {
                continue;
            }

            request.assets().forEach(assetRequest -> {
                QuestionAsset asset = toAsset(assetRequest);
                asset.setQuestion(question);
                asset.setAlternative(alternative);
                alternative.getAssets().add(asset);
                question.getAssets().add(asset);
            });
        }
    }
}
