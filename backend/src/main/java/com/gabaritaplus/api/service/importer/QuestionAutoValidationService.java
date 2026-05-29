package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.dto.importer.official.OfficialExamSourceRequest;
import com.gabaritaplus.api.dto.importer.official.OfficialExamSourceResponse;
import com.gabaritaplus.api.dto.importer.review.AutoValidationBatchResponse;
import com.gabaritaplus.api.dto.importer.review.AutoValidationCountersResponse;
import com.gabaritaplus.api.dto.importer.review.OfficialValidationItemResponse;
import com.gabaritaplus.api.dto.importer.review.OfficialValidationReportResponse;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;

    @Value("${app.import.auto-publish-imported-questions:false}")
    private boolean autoPublishImportedQuestions;

    @Transactional
    public OfficialExamSourceResponse createOfficialSource(OfficialExamSourceRequest request) {
        OfficialExamSource source = findExistingOfficialSource(request)
                .orElseGet(OfficialExamSource::new);
        source.setExam(request.exam());
        source.setYear(request.year());
        source.setDay(request.day());
        source.setBookColor(normalizeBookColor(request.bookColor()));
        source.setPdfUrl(request.pdfUrl());
        source.setAnswerKeyUrl(request.answerKeyUrl());
        source.setSourceUrl(request.sourceUrl());
        source.setLocalPdfPath(request.localPdfPath());
        source.setAnswerKeyMapJson(request.answerKeyMapJson());
        return toOfficialSourceResponse(officialExamSourceRepository.save(source));
    }

    @Transactional
    public void deleteOfficialSource(Long id) {
        OfficialExamSource source = officialExamSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fonte oficial nao encontrada."));
        officialExamSourceRepository.delete(source);
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

    @Transactional
    public OfficialValidationReportResponse validateAgainstOfficialSource(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Questao nao encontrada."));
        OfficialValidationItemResponse item = validateQuestionAgainstOfficialSource(question);
        questionRepository.save(question);
        return buildOfficialReport(List.of(item));
    }

    @Transactional
    public OfficialValidationReportResponse validateAgainstOfficialSourceBatch() {
        List<Question> candidates = reviewCandidates();
        List<OfficialValidationItemResponse> items = candidates.stream()
                .map(this::validateQuestionAgainstOfficialSource)
                .toList();
        questionRepository.saveAll(candidates);
        return buildOfficialReport(items);
    }

    @Transactional
    public OfficialValidationReportResponse recoverAssets(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Questao nao encontrada."));
        OfficialValidationItemResponse item = recoverQuestionAssets(question);
        questionRepository.save(question);
        return buildOfficialReport(List.of(item));
    }

    @Transactional
    public OfficialValidationReportResponse recoverAssetsBatch() {
        List<Question> candidates = reviewCandidates();
        List<OfficialValidationItemResponse> items = candidates.stream()
                .map(this::recoverQuestionAssets)
                .toList();
        questionRepository.saveAll(candidates);
        return buildOfficialReport(items);
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

    private OfficialValidationItemResponse validateQuestionAgainstOfficialSource(Question question) {
        initializeQuestionGraph(question);
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean previousValidated = Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource());
        Integer previousScore = question.getAutoValidationScore();
        String previousBookColor = question.getSourceBookColor();
        OfficialSourceMatch match = findMatchingOfficialSource(question);
        Optional<OfficialExamSource> source = match.source();
        warnings.addAll(match.warnings());
        errors.addAll(match.errors());

        if (source.isEmpty()) {
            if (!errors.contains("AMBIGUOUS_OFFICIAL_SOURCE")) {
                warnings.add("OFFICIAL_SOURCE_NOT_FOUND");
            }
        } else {
            attachOfficialSource(question, source.get());
            warnings.addAll(validateOfficialMetadata(question, source.get()));
            errors.addAll(validateOfficialAnswerKey(question, source.get()));
            if (isUnknownBookColor(previousBookColor) && source.get().getBookColor() != null) {
                question.setSourceBookColor(source.get().getBookColor());
                warnings.add("BOOK_COLOR_INFERRED_FROM_OFFICIAL_SOURCE");
            }
        }

        boolean canTrustOfficialValidation = source.isPresent() && errors.isEmpty();
        if (canTrustOfficialValidation) {
            question.setValidatedAgainstOfficialSource(true);
            question.setValidatedAt(OffsetDateTime.now());
            if (question.getImportStatus() == QuestionImportStatus.NEEDS_REVIEW) {
                question.setImportStatus(QuestionImportStatus.VALIDATED);
            }
        } else {
            question.setValidatedAgainstOfficialSource(false);
            if (question.getImportStatus() != QuestionImportStatus.INVALID
                    && question.getImportStatus() != QuestionImportStatus.PUBLISHED) {
                question.setImportStatus(QuestionImportStatus.NEEDS_REVIEW);
            }
        }

        applyAutoValidation(question);
        appendWarnings(question, warnings);
        appendErrors(question, errors);
        return toOfficialItem(question, false, previousValidated, previousScore, warnings, errors);
    }

    private OfficialValidationItemResponse recoverQuestionAssets(Question question) {
        initializeQuestionGraph(question);
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean previousValidated = Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource());
        Integer previousScore = question.getAutoValidationScore();
        OfficialSourceMatch match = findMatchingOfficialSource(question);
        Optional<OfficialExamSource> source = match.source();
        warnings.addAll(match.warnings());
        errors.addAll(match.errors());
        boolean assetRecovered = false;

        if (source.isEmpty()) {
            if (!errors.contains("AMBIGUOUS_OFFICIAL_SOURCE")) {
                warnings.add("OFFICIAL_SOURCE_NOT_FOUND");
                warnings.add("PDF_NOT_FOUND");
            }
        } else {
            attachOfficialSource(question, source.get());
            if (source.get().getPdfUrl() == null || source.get().getPdfUrl().isBlank()) {
                warnings.add("PDF_NOT_FOUND");
            } else if (hasBrokenImageReference(question) || (mentionsVisualAsset(question) && question.getAssets().isEmpty())) {
                warnings.add("ASSET_RECOVERY_FAILED");
            }
        }

        applyAutoValidation(question);
        appendWarnings(question, warnings);
        appendErrors(question, errors);
        return toOfficialItem(question, assetRecovered, previousValidated, previousScore, warnings, errors);
    }

    private List<Question> reviewCandidates() {
        return questionRepository.findByImportStatusIn(List.of(
                QuestionImportStatus.DRAFT,
                QuestionImportStatus.NEEDS_REVIEW,
                QuestionImportStatus.VALIDATED,
                QuestionImportStatus.AUTO_VALIDATED,
                QuestionImportStatus.INVALID
        ));
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

        findMatchingOfficialSource(question).source().ifPresent(source -> attachOfficialSource(question, source));
    }

    private OfficialSourceMatch findMatchingOfficialSource(Question question) {
        if (question.getSourceExam() == null || question.getSourceYear() == null || question.getSourceDay() == null) {
            return new OfficialSourceMatch(Optional.empty(), List.of(), List.of());
        }

        List<OfficialExamSource> candidates = officialExamSourceRepository
                .findByExamIgnoreCaseAndYearAndDay(question.getSourceExam(), question.getSourceYear(), question.getSourceDay())
                .stream()
                .toList();

        if (isUnknownBookColor(question.getSourceBookColor())) {
            if (candidates.size() == 1) {
                return new OfficialSourceMatch(Optional.of(candidates.getFirst()), List.of("BOOK_COLOR_INFERRED_FROM_OFFICIAL_SOURCE"), List.of());
            }
            if (candidates.size() > 1) {
                return new OfficialSourceMatch(Optional.empty(), List.of(), List.of("AMBIGUOUS_OFFICIAL_SOURCE"));
            }
            return new OfficialSourceMatch(Optional.empty(), List.of(), List.of());
        }

        List<OfficialExamSource> colorMatches = candidates.stream()
                .filter(source -> source.getBookColor() != null
                        && source.getBookColor().equalsIgnoreCase(question.getSourceBookColor()))
                .toList();
        if (colorMatches.size() == 1) {
            return new OfficialSourceMatch(Optional.of(colorMatches.getFirst()), List.of(), List.of());
        }
        if (colorMatches.size() > 1) {
            return new OfficialSourceMatch(Optional.empty(), List.of(), List.of("AMBIGUOUS_OFFICIAL_SOURCE"));
        }
        return new OfficialSourceMatch(Optional.empty(), List.of(), List.of());
    }

    private void attachOfficialSource(Question question, OfficialExamSource source) {
        question.setOfficialPdfUrl(source.getPdfUrl());
        question.setOfficialAnswerKeyUrl(source.getAnswerKeyUrl());
        question.setOfficialSourceUrl(source.getSourceUrl());
    }

    private List<String> validateOfficialMetadata(Question question, OfficialExamSource source) {
        List<String> warnings = new ArrayList<>();
        if (!source.getYear().equals(question.getSourceYear())) {
            warnings.add("OFFICIAL_YEAR_MISMATCH");
        }
        if (source.getDay() == null || question.getSourceDay() == null || !source.getDay().equals(question.getSourceDay())) {
            warnings.add("OFFICIAL_DAY_MISMATCH");
        }
        if (question.getSourceQuestionNumber() == null) {
            warnings.add("OFFICIAL_QUESTION_NUMBER_MISSING");
        }
        if (source.getBookColor() != null
                && question.getSourceBookColor() != null
                && !"UNKNOWN".equalsIgnoreCase(question.getSourceBookColor())
                && !source.getBookColor().equalsIgnoreCase(question.getSourceBookColor())) {
            warnings.add("OFFICIAL_BOOK_COLOR_MISMATCH");
        }
        if (source.getPdfUrl() == null || source.getPdfUrl().isBlank()) {
            warnings.add("PDF_NOT_FOUND");
        }
        return warnings;
    }

    private List<String> validateOfficialAnswerKey(Question question, OfficialExamSource source) {
        List<String> errors = new ArrayList<>();
        Map<String, String> answerKey = parseAnswerKeyMap(source.getAnswerKeyMapJson());
        if (answerKey.isEmpty()) {
            errors.add("ANSWER_KEY_MISSING");
            return errors;
        }

        String officialAnswer = answerKey.get(String.valueOf(question.getSourceQuestionNumber()));
        if (officialAnswer == null || officialAnswer.isBlank()) {
            errors.add("ANSWER_KEY_MISSING");
            return errors;
        }

        String importedAnswer = question.getCorrectAlternative() == null
                ? ""
                : question.getCorrectAlternative().trim().toUpperCase(Locale.ROOT);
        if (!officialAnswer.trim().equalsIgnoreCase(importedAnswer)) {
            errors.add("ANSWER_KEY_MISMATCH");
        }
        return errors;
    }

    private Map<String, String> parseAnswerKeyMap(String answerKeyMapJson) {
        if (answerKeyMapJson == null || answerKeyMapJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(answerKeyMapJson, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void appendWarnings(Question question, List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        List<String> merged = new ArrayList<>();
        if (question.getAutoValidationWarnings() != null && !question.getAutoValidationWarnings().isBlank()) {
            merged.addAll(List.of(question.getAutoValidationWarnings().split("\\n+")));
        }
        merged.addAll(warnings);
        question.setAutoValidationWarnings(String.join("\n", merged.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList()));
    }

    private void appendErrors(Question question, List<String> errors) {
        if (errors.isEmpty()) {
            return;
        }
        List<String> merged = new ArrayList<>();
        if (question.getAutoValidationErrors() != null && !question.getAutoValidationErrors().isBlank()) {
            merged.addAll(List.of(question.getAutoValidationErrors().split("\\n+")));
        }
        merged.addAll(errors);
        question.setAutoValidationErrors(String.join("\n", merged.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList()));
    }

    private OfficialValidationItemResponse toOfficialItem(
            Question question,
            boolean assetRecovered,
            boolean previousValidated,
            Integer previousScore,
            List<String> warnings,
            List<String> errors
    ) {
        List<String> mergedWarnings = new ArrayList<>();
        if (question.getAutoValidationWarnings() != null && !question.getAutoValidationWarnings().isBlank()) {
            mergedWarnings.addAll(List.of(question.getAutoValidationWarnings().split("\\n+")));
        }
        mergedWarnings.addAll(warnings);
        return new OfficialValidationItemResponse(
                question.getId(),
                question.getTitle(),
                question.getSourceQuestionNumber(),
                previousValidated,
                Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource()),
                previousScore,
                question.getAutoValidationScore(),
                question.getImportStatus(),
                question.getAutoValidationStatus(),
                Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource()),
                assetRecovered,
                previousValidated != Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource())
                        || !java.util.Objects.equals(previousScore, question.getAutoValidationScore()),
                List.copyOf(mergedWarnings.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList()),
                List.copyOf(errors.stream().distinct().toList())
        );
    }

    private OfficialValidationReportResponse buildOfficialReport(List<OfficialValidationItemResponse> items) {
        return new OfficialValidationReportResponse(
                items.size(),
                items.size(),
                (int) items.stream().filter(OfficialValidationItemResponse::validatedAgainstOfficialSource).count(),
                (int) items.stream().filter(item -> !item.updated() && item.errors().isEmpty()).count(),
                (int) items.stream().filter(item -> !item.errors().isEmpty()).count(),
                (int) items.stream().filter(item -> item.errors().contains("AMBIGUOUS_OFFICIAL_SOURCE")).count(),
                (int) items.stream().filter(item -> item.errors().contains("ANSWER_KEY_MISSING")).count(),
                (int) items.stream().filter(item -> item.errors().contains("ANSWER_KEY_MISMATCH")).count(),
                (int) items.stream().filter(OfficialValidationItemResponse::updated).count(),
                (int) items.stream().filter(item -> item.importStatus() == QuestionImportStatus.NEEDS_REVIEW).count(),
                (int) items.stream().filter(item -> item.importStatus() == QuestionImportStatus.INVALID).count(),
                (int) items.stream().filter(item -> item.warnings().contains("ASSET_MISSING_OR_BROKEN")).count(),
                (int) items.stream().filter(item -> item.warnings().contains("ASSET_RECOVERY_FAILED")).count(),
                (int) items.stream().filter(item -> !item.validatedAgainstOfficialSource()).count(),
                (int) items.stream().filter(OfficialValidationItemResponse::assetRecovered).count(),
                (int) items.stream().filter(item -> item.warnings().contains("ASSET_RECOVERY_FAILED")).count(),
                items
        );
    }

    private Optional<OfficialExamSource> findExistingOfficialSource(OfficialExamSourceRequest request) {
        return officialExamSourceRepository.findByExamIgnoreCaseAndYearAndDay(request.exam(), request.year(), request.day())
                .stream()
                .filter(source -> java.util.Objects.equals(normalizeBookColor(source.getBookColor()), normalizeBookColor(request.bookColor())))
                .findFirst();
    }

    private String normalizeBookColor(String bookColor) {
        if (bookColor == null || bookColor.isBlank()) {
            return null;
        }
        return bookColor.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isUnknownBookColor(String bookColor) {
        return bookColor == null || bookColor.isBlank() || "UNKNOWN".equalsIgnoreCase(bookColor);
    }

    private record OfficialSourceMatch(
            Optional<OfficialExamSource> source,
            List<String> warnings,
            List<String> errors
    ) {
    }

    private void initializeQuestionGraph(Question question) {
        question.getAssets().size();
        question.getAlternatives().forEach(alternative -> alternative.getAssets().size());
        question.getAlternatives().size();
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
                source.getAnswerKeyMapJson(),
                source.getCreatedAt()
        );
    }
}
