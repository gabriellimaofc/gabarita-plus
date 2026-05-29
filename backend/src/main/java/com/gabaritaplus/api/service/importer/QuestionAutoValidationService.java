package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.dto.importer.official.OfficialExamSourceRequest;
import com.gabaritaplus.api.dto.importer.official.OfficialExamSourceResponse;
import com.gabaritaplus.api.dto.importer.review.AutoValidationBatchResponse;
import com.gabaritaplus.api.dto.importer.review.AutoValidationCountersResponse;
import com.gabaritaplus.api.entity.Alternative;
import com.gabaritaplus.api.entity.OfficialExamSource;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.QuestionAsset;
import com.gabaritaplus.api.entity.enums.AutoValidationStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.exception.ResourceNotFoundException;
import com.gabaritaplus.api.repository.OfficialExamSourceRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionAutoValidationService {

    private static final Set<String> VALID_LETTERS = Set.of("A", "B", "C", "D", "E");
    private static final List<String> VISUAL_KEYWORDS = List.of(
            "grafico", "gráfico", "figura", "imagem", "mapa", "tabela", "cartaz",
            "charge", "tirinha", "esquema", "desenho", "ilustracao", "ilustração",
            "diagrama", "observe", "conforme mostrado", "a seguir"
    );
    private static final List<String> SUSPICIOUS_MARKERS = List.of(
            "Ã", "â€œ", "â€", "â€“", "ТЕХТО", "текст"
    );

    private final QuestionRepository questionRepository;
    private final OfficialExamSourceRepository officialExamSourceRepository;

    @Value("${app.import.auto-publish-imported-questions:false}")
    private boolean autoPublishImportedQuestions;

    @Transactional
    public OfficialExamSourceResponse createOfficialSource(OfficialExamSourceRequest request) {
        OfficialExamSource source = new OfficialExamSource();
        source.setExam(request.exam());
        source.setYear(request.year());
        source.setDay(request.day());
        source.setBookColor(request.bookColor());
        source.setPdfUrl(request.pdfUrl());
        source.setAnswerKeyUrl(request.answerKeyUrl());
        source.setSourceUrl(request.sourceUrl());
        source.setLocalPdfPath(request.localPdfPath());
        return toOfficialSourceResponse(officialExamSourceRepository.save(source));
    }

    @Transactional(readOnly = true)
    public List<OfficialExamSourceResponse> listOfficialSources() {
        return officialExamSourceRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toOfficialSourceResponse)
                .toList();
    }

    @Transactional
    public Question autoValidate(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Questao nao encontrada."));
        applyAutoValidation(question);
        return questionRepository.save(question);
    }

    @Transactional
    public AutoValidationBatchResponse autoValidateBatch() {
        List<Question> candidates = questionRepository.findByImportStatusIn(List.of(
                QuestionImportStatus.DRAFT,
                QuestionImportStatus.NEEDS_REVIEW,
                QuestionImportStatus.VALIDATED,
                QuestionImportStatus.AUTO_VALIDATED,
                QuestionImportStatus.INVALID
        ));
        int safe = 0;
        int needsReview = 0;
        int invalid = 0;
        int published = 0;

        for (Question question : candidates) {
            applyAutoValidation(question);
            if (question.getAutoValidationStatus() == AutoValidationStatus.SAFE_TO_AUTO_VALIDATE) {
                safe++;
            } else if (question.getAutoValidationStatus() == AutoValidationStatus.AUTO_INVALID) {
                invalid++;
            } else {
                needsReview++;
            }
            if (question.getImportStatus() == QuestionImportStatus.PUBLISHED) {
                published++;
            }
        }
        questionRepository.saveAll(candidates);
        return new AutoValidationBatchResponse(candidates.size(), safe, needsReview, invalid, published);
    }

    @Transactional
    public AutoValidationBatchResponse autoPublishSafe() {
        List<Question> candidates = questionRepository.findByImportStatusIn(List.of(
                QuestionImportStatus.AUTO_VALIDATED,
                QuestionImportStatus.VALIDATED,
                QuestionImportStatus.NEEDS_REVIEW
        ));
        int safe = 0;
        int needsReview = 0;
        int invalid = 0;
        int published = 0;

        for (Question question : candidates) {
            applyAutoValidation(question);
            if (question.getAutoValidationStatus() == AutoValidationStatus.SAFE_TO_AUTO_VALIDATE) {
                safe++;
            } else if (question.getAutoValidationStatus() == AutoValidationStatus.AUTO_INVALID) {
                invalid++;
            } else {
                needsReview++;
            }

            if (canAutoPublish(question)) {
                question.setImportStatus(QuestionImportStatus.PUBLISHED);
                published++;
            }
        }
        questionRepository.saveAll(candidates);
        return new AutoValidationBatchResponse(candidates.size(), safe, needsReview, invalid, published);
    }

    @Transactional(readOnly = true)
    public AutoValidationCountersResponse counters() {
        List<Question> reviewable = questionRepository.findByImportStatusIn(List.of(
                QuestionImportStatus.DRAFT,
                QuestionImportStatus.NEEDS_REVIEW,
                QuestionImportStatus.VALIDATED,
                QuestionImportStatus.AUTO_VALIDATED,
                QuestionImportStatus.INVALID
        ));
        return new AutoValidationCountersResponse(
                reviewable.stream().filter(question -> question.getAutoValidationStatus() == AutoValidationStatus.SAFE_TO_AUTO_VALIDATE).count(),
                reviewable.stream().filter(question -> question.getAutoValidationStatus() == AutoValidationStatus.NEEDS_HUMAN_REVIEW).count(),
                reviewable.stream().filter(question -> question.getAutoValidationStatus() == AutoValidationStatus.AUTO_INVALID).count(),
                reviewable.stream().filter(question -> Boolean.TRUE.equals(question.getBrokenImageDetected())).count(),
                reviewable.stream().filter(question -> !Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource())).count()
        );
    }

    public AutoValidationResult evaluate(Question question) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int score = 0;

        boolean hasStatement = question.getStatement() != null && !question.getStatement().isBlank();
        boolean hasFiveAlternatives = question.getAlternatives().size() == 5;
        boolean hasValidAnswer = question.getCorrectAlternative() != null
                && VALID_LETTERS.contains(question.getCorrectAlternative().trim().toUpperCase(Locale.ROOT));
        boolean brokenImage = hasBrokenImageReference(question);
        boolean suspiciousText = hasSuspiciousText(question);
        boolean requiredOriginFields = question.getSourceYear() != null
                && question.getSourceQuestionNumber() != null
                && question.getSourceDay() != null;
        boolean requiresAssetReview = mentionsVisualAsset(question) && question.getAssets().isEmpty();
        boolean officialValidated = Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource());

        if (hasFiveAlternatives) {
            score += 15;
        } else {
            errors.add("ALTERNATIVES_INVALID");
        }
        if (hasValidAnswer) {
            score += 15;
        } else {
            errors.add("ANSWER_KEY_MISSING_OR_INVALID");
        }
        if (hasStatement) {
            score += 10;
        } else {
            errors.add("STATEMENT_MISSING");
        }
        if (!brokenImage) {
            score += 15;
        } else {
            warnings.add("ASSET_MISSING_OR_BROKEN");
        }
        if (!suspiciousText) {
            score += 10;
        } else {
            warnings.add("SUSPICIOUS_TEXT_DETECTED");
        }
        if (requiredOriginFields) {
            score += 10;
        } else {
            warnings.add("SOURCE_METADATA_INCOMPLETE");
        }
        if (!requiresAssetReview) {
            score += 15;
        } else {
            warnings.add("ASSET_MISSING_OR_BROKEN");
        }
        if (officialValidated) {
            score += 10;
        } else {
            warnings.add("PENDING_OFFICIAL_INEP_VALIDATION");
        }
        if ("UNKNOWN".equalsIgnoreCase(String.valueOf(question.getSourceBookColor()))) {
            warnings.add("SOURCE_BOOK_COLOR_UNKNOWN");
        }

        AutoValidationStatus status;
        if (!hasFiveAlternatives || !hasValidAnswer || !hasStatement || hasEmptyAlternative(question)) {
            status = AutoValidationStatus.AUTO_INVALID;
        } else if (brokenImage || suspiciousText || requiresAssetReview || !officialValidated
                || !requiredOriginFields || warnings.contains("SOURCE_BOOK_COLOR_UNKNOWN")) {
            status = AutoValidationStatus.NEEDS_HUMAN_REVIEW;
        } else {
            status = AutoValidationStatus.SAFE_TO_AUTO_VALIDATE;
        }

        return new AutoValidationResult(
                Math.min(score, 100),
                status,
                List.copyOf(errors),
                List.copyOf(warnings.stream().distinct().toList()),
                brokenImage,
                suspiciousText,
                requiresAssetReview
        );
    }

    public void applyAutoValidation(Question question) {
        tryAttachOfficialSource(question);
        AutoValidationResult result = evaluate(question);
        question.setAutoValidationScore(result.score());
        question.setAutoValidationStatus(result.status());
        question.setAutoValidationErrors(String.join("\n", result.errors()));
        question.setAutoValidationWarnings(String.join("\n", result.warnings()));
        question.setAutoValidatedAt(OffsetDateTime.now());
        question.setBrokenImageDetected(result.brokenImageDetected());
        question.setSuspiciousTextDetected(result.suspiciousTextDetected());
        question.setRequiresAssetReview(result.requiresAssetReview());

        if (result.status() == AutoValidationStatus.AUTO_INVALID) {
            question.setImportStatus(QuestionImportStatus.INVALID);
        } else if (result.status() == AutoValidationStatus.SAFE_TO_AUTO_VALIDATE
                && question.getImportStatus() != QuestionImportStatus.PUBLISHED) {
            question.setImportStatus(QuestionImportStatus.AUTO_VALIDATED);
        } else if (question.getImportStatus() != QuestionImportStatus.PUBLISHED) {
            question.setImportStatus(QuestionImportStatus.NEEDS_REVIEW);
        }

        if (canAutoPublish(question)) {
            question.setImportStatus(QuestionImportStatus.PUBLISHED);
        }
    }

    public boolean canAutoPublish(Question question) {
        return autoPublishImportedQuestions
                && question.getAutoValidationScore() != null
                && question.getAutoValidationScore() >= 95
                && Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource())
                && question.getCorrectAlternative() != null
                && question.getAlternatives().size() == 5
                && !Boolean.TRUE.equals(question.getBrokenImageDetected())
                && !Boolean.TRUE.equals(question.getSuspiciousTextDetected())
                && !Boolean.TRUE.equals(question.getRequiresAssetReview());
    }

    private void tryAttachOfficialSource(Question question) {
        if (question.getOfficialPdfUrl() != null || question.getSourceExam() == null
                || question.getSourceYear() == null || question.getSourceDay() == null) {
            return;
        }

        officialExamSourceRepository
                .findByExamIgnoreCaseAndYearAndDay(question.getSourceExam(), question.getSourceYear(), question.getSourceDay())
                .stream()
                .filter(source -> source.getBookColor() == null
                        || question.getSourceBookColor() == null
                        || source.getBookColor().equalsIgnoreCase(question.getSourceBookColor()))
                .findFirst()
                .ifPresent(source -> {
                    question.setOfficialPdfUrl(source.getPdfUrl());
                    question.setOfficialAnswerKeyUrl(source.getAnswerKeyUrl());
                    question.setOfficialSourceUrl(source.getSourceUrl());
                });
    }

    private boolean hasEmptyAlternative(Question question) {
        return question.getAlternatives().stream()
                .anyMatch(alternative -> alternative.getText() == null || alternative.getText().isBlank());
    }

    private boolean hasBrokenImageReference(Question question) {
        List<String> values = new ArrayList<>();
        values.add(question.getImageUrl());
        values.add(question.getStatement());
        values.add(question.getStatementHtml());
        question.getAssets().forEach(asset -> addAssetValues(values, asset));
        question.getAlternatives().stream()
                .flatMap(alternative -> alternative.getAssets().stream())
                .forEach(asset -> addAssetValues(values, asset));
        return values.stream()
                .filter(value -> value != null)
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains("broken-image"));
    }

    private void addAssetValues(List<String> values, QuestionAsset asset) {
        values.add(asset.getUrl());
        values.add(asset.getStoragePath());
    }

    private boolean hasSuspiciousText(Question question) {
        String content = (question.getTitle() + " " + question.getStatement() + " " + question.getStatementHtml());
        boolean marker = SUSPICIOUS_MARKERS.stream().anyMatch(content::contains);
        boolean cyrillic = content.codePoints().anyMatch(codePoint -> codePoint >= 0x0400 && codePoint <= 0x04FF);
        return marker || cyrillic;
    }

    private boolean mentionsVisualAsset(Question question) {
        String content = (question.getStatement() + " " + question.getStatementHtml()).toLowerCase(Locale.ROOT);
        return VISUAL_KEYWORDS.stream().anyMatch(content::contains);
    }

    private OfficialExamSourceResponse toOfficialSourceResponse(OfficialExamSource source) {
        return new OfficialExamSourceResponse(
                source.getId(),
                source.getExam(),
                source.getYear(),
                source.getDay(),
                source.getBookColor(),
                source.getPdfUrl(),
                source.getAnswerKeyUrl(),
                source.getSourceUrl(),
                source.getLocalPdfPath(),
                source.getCreatedAt()
        );
    }
}
