package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.dto.importer.ImportAlternativePayload;
import com.gabaritaplus.api.dto.importer.ImportQuestionAssetPayload;
import com.gabaritaplus.api.dto.importer.ImportQuestionPayload;
import com.gabaritaplus.api.dto.importer.ImportReportResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevAlternativeResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevExamResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevImportRequest;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevLabelValueResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevPreviewItemResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevPreviewResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevYearResponse;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.QuestionAssetType;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EnemDevImportService {

    private static final int DEFAULT_SAMPLE_LIMIT = 1;
    private static final int MAX_SAMPLE_LIMIT = 10;
    private static final String ENEM_DEV_SOURCE = "ENEM_DEV";
    private static final String ENEM_DEV_PROVIDER = "enem.dev";
    private static final String ENEM_DEV_PROVIDER_URL = "https://enem.dev";
    private static final String ENEM_DEV_API_URL = "https://api.enem.dev/v1";

    private final EnemDevApiClient enemDevApiClient;
    private final QuestionImportSupport questionImportSupport;
    private final QuestionImportService questionImportService;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<EnemDevYearResponse> listYears() {
        return enemDevApiClient.listExams().stream()
                .map(this::toYearResponse)
                .sorted((left, right) -> right.year().compareTo(left.year()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EnemDevPreviewResponse preview(EnemDevImportRequest request) {
        List<ImportQuestionPayload> normalized = fetchAndNormalizeSample(request);
        List<EnemDevPreviewItemResponse> items = new ArrayList<>();

        for (ImportQuestionPayload payload : normalized) {
            List<String> warnings = new ArrayList<>(questionImportSupport.validateImportPayload(
                    payload,
                    ImportValidationMode.EXTERNAL_AUXILIARY
            ));
            String statementHash = questionImportSupport.generateStatementHash(payload.statement(), payload.statementHtml());
            QuestionImportStatus status = questionImportSupport.determineStatus(
                    payload,
                    warnings,
                    ImportValidationMode.EXTERNAL_AUXILIARY
            );
            boolean duplicate = isDuplicate(payload, statementHash);
            if (duplicate) {
                warnings.add("Questao ja identificada como duplicada na base.");
            }
            if (status == QuestionImportStatus.NEEDS_REVIEW) {
                warnings.add("Questao externa mantida em NEEDS_REVIEW ate validacao oficial no INEP.");
            }
            items.add(new EnemDevPreviewItemResponse(payload, statementHash, status, duplicate, List.copyOf(warnings)));
        }

        return new EnemDevPreviewResponse(
                request.year(),
                normalized.size(),
                items.size(),
                items
        );
    }

    @Transactional(readOnly = true)
    public ImportReportResponse dryRun(EnemDevImportRequest request) {
        return questionImportService.dryRunQuestions(fetchAndNormalizeSample(request), ImportValidationMode.EXTERNAL_AUXILIARY);
    }

    @Transactional
    public ImportReportResponse importQuestions(EnemDevImportRequest request) {
        return questionImportService.importQuestions(fetchAndNormalize(request), ImportValidationMode.EXTERNAL_AUXILIARY);
    }

    public List<ImportQuestionPayload> fetchAndNormalize(EnemDevImportRequest request) {
        return enemDevApiClient.listAllQuestions(
                        request.year(),
                        request.limit(),
                        request.offset(),
                        request.language()
                ).stream()
                .map(question -> normalizeQuestion(request.year(), question))
                .toList();
    }

    public List<ImportQuestionPayload> fetchAndNormalizeSample(EnemDevImportRequest request) {
        int limit = sanitizeSampleLimit(request.limit());
        return enemDevApiClient.listQuestions(
                        request.year(),
                        limit,
                        request.offset(),
                        request.language()
                ).stream()
                .map(question -> normalizeQuestion(request.year(), question))
                .toList();
    }

    private EnemDevYearResponse toYearResponse(EnemDevExamResponse exam) {
        return new EnemDevYearResponse(
                exam.year(),
                exam.title(),
                mapLabels(exam.disciplines()),
                mapLabels(exam.languages()),
                exam.questions() == null ? null : exam.questions().size()
        );
    }

    private List<String> mapLabels(List<EnemDevLabelValueResponse> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(item -> item.label() == null || item.label().isBlank() ? item.value() : item.label())
                .filter(Objects::nonNull)
                .toList();
    }

    private ImportQuestionPayload normalizeQuestion(Integer requestedYear, EnemDevQuestionResponse question) {
        String statement = combineStatement(question.context(), question.alternativesIntroduction());
        String language = normalizeOptional(question.language());
        String discipline = normalizeOptional(question.discipline());
        String questionUrl = ENEM_DEV_API_URL + "/exams/" + requestedYear + "/questions/" + question.index();
        List<ImportQuestionAssetPayload> questionAssets = mapQuestionAssets(question.files());
        List<ImportAlternativePayload> alternatives = mapAlternatives(question.alternatives());
        String correctAlternative = resolveCorrectAlternative(question, alternatives);

        return new ImportQuestionPayload(
                normalizeTitle(question.title(), requestedYear, question.index()),
                statement,
                null,
                firstFile(question.files()),
                normalizeSubject(discipline),
                "A classificar",
                normalizeSubtopic(language, discipline),
                DifficultyLevel.MEDIUM,
                requestedYear,
                "ENEM",
                null,
                null,
                null,
                correctAlternative,
                ENEM_DEV_SOURCE,
                questionUrl,
                "ENEM",
                requestedYear,
                question.index(),
                "UNKNOWN",
                inferDay(discipline),
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                ENEM_DEV_PROVIDER,
                ENEM_DEV_PROVIDER_URL,
                buildExternalQuestionId(requestedYear, question.index(), discipline, language),
                null,
                questionAssets,
                alternatives
        );
    }

    private List<ImportQuestionAssetPayload> mapQuestionAssets(List<String> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(Objects::nonNull)
                .map(file -> new ImportQuestionAssetPayload(
                        QuestionAssetType.IMAGE,
                        file,
                        null,
                        file.substring(file.lastIndexOf('/') + 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Asset externo referenciado pela API enem.dev.",
                        null,
                        null
                ))
                .toList();
    }

    private List<ImportAlternativePayload> mapAlternatives(List<EnemDevAlternativeResponse> alternatives) {
        if (alternatives == null) {
            return List.of();
        }
        return alternatives.stream()
                .map(item -> new ImportAlternativePayload(
                        normalizeLetter(item.letter()),
                        normalizeText(item.text()),
                        null,
                        item.file() == null || item.file().isBlank()
                                ? List.of()
                                : List.of(new ImportQuestionAssetPayload(
                                QuestionAssetType.IMAGE,
                                item.file(),
                                null,
                                item.file().substring(item.file().lastIndexOf('/') + 1),
                                null,
                                null,
                                null,
                                null,
                                null,
                                "Asset externo vinculado a alternativa na API enem.dev.",
                                null,
                                null
                        ))
                ))
                .toList();
    }

    private String resolveCorrectAlternative(
            EnemDevQuestionResponse question,
            List<ImportAlternativePayload> alternatives
    ) {
        if (question.correctAlternative() != null && !question.correctAlternative().isBlank()) {
            return normalizeLetter(question.correctAlternative());
        }
        return question.alternatives() == null
                ? null
                : question.alternatives().stream()
                .filter(item -> Boolean.TRUE.equals(item.isCorrect()))
                .map(EnemDevAlternativeResponse::letter)
                .map(this::normalizeLetter)
                .findFirst()
                .orElse(null);
    }

    private boolean isDuplicate(ImportQuestionPayload payload, String statementHash) {
        boolean sameExternalIdentity = payload.externalProvider() != null
                && payload.externalQuestionId() != null
                && questionRepository.existsByExternalProviderIgnoreCaseAndExternalQuestionIdIgnoreCase(
                payload.externalProvider(),
                payload.externalQuestionId()
        );
        boolean sameSourceIdentity = payload.sourceDay() != null
                && payload.sourceBookColor() != null
                && questionRepository.existsBySourceExamIgnoreCaseAndSourceYearAndSourceQuestionNumberAndSourceDayAndSourceBookColorIgnoreCase(
                payload.sourceExam(),
                payload.sourceYear(),
                payload.sourceQuestionNumber(),
                payload.sourceDay(),
                payload.sourceBookColor()
        );
        boolean sameHash = questionRepository.existsByStatementHashAndSourceExamIgnoreCaseAndSourceYear(
                statementHash,
                payload.sourceExam(),
                payload.sourceYear()
        );
        return sameExternalIdentity || sameSourceIdentity || sameHash;
    }

    private String normalizeTitle(String title, Integer year, Integer index) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return "ENEM " + year + " - Questao " + index;
    }

    private String combineStatement(String context, String alternativesIntroduction) {
        String normalizedContext = normalizeText(context);
        String normalizedAlternativesIntroduction = normalizeText(alternativesIntroduction);
        if (normalizedContext == null && normalizedAlternativesIntroduction == null) {
            return "";
        }
        if (normalizedContext == null) {
            return normalizedAlternativesIntroduction;
        }
        if (normalizedAlternativesIntroduction == null) {
            return normalizedContext;
        }
        return normalizedContext + "\n\n" + normalizedAlternativesIntroduction;
    }

    private String normalizeSubject(String discipline) {
        if (discipline == null) {
            return "A classificar";
        }
        return switch (discipline.toLowerCase(Locale.ROOT)) {
            case "ciencias-humanas" -> "Ciencias Humanas";
            case "ciencias-natureza" -> "Ciencias da Natureza";
            case "linguagens" -> "Linguagens";
            case "matematica" -> "Matematica";
            default -> "A classificar";
        };
    }

    private String normalizeSubtopic(String language, String discipline) {
        if (language != null && !language.isBlank()) {
            return language.substring(0, 1).toUpperCase(Locale.ROOT) + language.substring(1).toLowerCase(Locale.ROOT);
        }
        if (discipline != null && !discipline.isBlank()) {
            return discipline;
        }
        return "A classificar";
    }

    private Integer inferDay(String discipline) {
        if (discipline == null) {
            return null;
        }
        return switch (discipline.toLowerCase(Locale.ROOT)) {
            case "linguagens", "ciencias-humanas" -> 1;
            case "matematica", "ciencias-natureza" -> 2;
            default -> null;
        };
    }

    private String buildExternalQuestionId(Integer year, Integer index, String discipline, String language) {
        return year + ":" + index + ":" + Objects.toString(discipline, "unknown") + ":" + Objects.toString(language, "default");
    }

    private String firstFile(List<String> files) {
        return files == null || files.isEmpty() ? null : files.getFirst();
    }

    private String normalizeLetter(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeText(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized;
    }

    private int sanitizeSampleLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_SAMPLE_LIMIT;
        }
        return Math.min(requestedLimit, MAX_SAMPLE_LIMIT);
    }
}
