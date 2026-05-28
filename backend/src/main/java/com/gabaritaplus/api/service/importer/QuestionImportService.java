package com.gabaritaplus.api.service.importer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabaritaplus.api.dto.importer.ImportAlternativePayload;
import com.gabaritaplus.api.dto.importer.ImportBatchResponse;
import com.gabaritaplus.api.dto.importer.ImportItemErrorResponse;
import com.gabaritaplus.api.dto.importer.ImportQuestionAssetPayload;
import com.gabaritaplus.api.dto.importer.ImportQuestionPayload;
import com.gabaritaplus.api.dto.importer.ImportQuestionsPayload;
import com.gabaritaplus.api.dto.importer.ImportReportResponse;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.entity.ImportBatch;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.enums.ImportBatchStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.mapper.QuestionMapper;
import com.gabaritaplus.api.repository.ImportBatchRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private static final TypeReference<List<ImportQuestionPayload>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ImportAlternativePayload>> ALTERNATIVE_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ImportQuestionAssetPayload>> ASSET_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final QuestionRepository questionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final QuestionMapper questionMapper;
    private final QuestionImportSupport questionImportSupport;

    @Transactional(readOnly = true)
    public ImportReportResponse dryRun(ImportQuestionsPayload payload) {
        return process(payload.questions(), true, ImportValidationMode.OFFICIAL);
    }

    @Transactional(readOnly = true)
    public ImportReportResponse dryRunQuestions(List<ImportQuestionPayload> payloads, ImportValidationMode mode) {
        return process(payloads, true, mode);
    }

    @Transactional
    public ImportReportResponse importJson(MultipartFile file) {
        return process(readJson(file), false, ImportValidationMode.OFFICIAL);
    }

    @Transactional
    public ImportReportResponse importCsv(MultipartFile file) {
        return process(readCsv(file), false, ImportValidationMode.OFFICIAL);
    }

    @Transactional
    public ImportReportResponse importQuestions(List<ImportQuestionPayload> payloads, ImportValidationMode mode) {
        return process(payloads, false, mode);
    }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> listBatches() {
        return importBatchRepository.findAll().stream()
                .sorted((left, right) -> right.getStartedAt().compareTo(left.getStartedAt()))
                .map(this::toBatchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportBatchResponse getBatch(Long id) {
        ImportBatch batch = importBatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lote de importacao nao encontrado."));
        return toBatchResponse(batch);
    }

    private ImportReportResponse process(List<ImportQuestionPayload> payloads, boolean dryRun, ImportValidationMode mode) {
        ImportBatch batch = null;
        if (!dryRun) {
            batch = startBatch(payloads);
        }

        int imported = 0;
        int skippedDuplicates = 0;
        int errors = 0;
        int needsReview = 0;
        int invalid = 0;
        List<ImportItemErrorResponse> itemErrors = new ArrayList<>();

        for (int index = 0; index < payloads.size(); index++) {
            ImportQuestionPayload payload = payloads.get(index);
            List<String> itemMessages = new ArrayList<>();
            itemMessages.addAll(validator.validate(payload).stream().map(ConstraintViolation::getMessage).toList());
            itemMessages.addAll(questionImportSupport.validateImportPayload(payload, mode));

            QuestionImportStatus status = questionImportSupport.determineStatus(payload, itemMessages, mode);
            String statementHash = questionImportSupport.generateStatementHash(payload.statement(), payload.statementHtml());

            if (isDuplicate(payload, statementHash)) {
                skippedDuplicates++;
                itemMessages.add("Questao ignorada por duplicidade detectada na base.");
                itemErrors.add(toItemError(index, payload, itemMessages));
                continue;
            }

            if (status == QuestionImportStatus.INVALID) {
                errors++;
                invalid++;
                itemErrors.add(toItemError(index, payload, itemMessages));
                continue;
            }

            if (status == QuestionImportStatus.NEEDS_REVIEW) {
                needsReview++;
                itemMessages.add("Questao marcada como NEEDS_REVIEW por completude ou texto suspeito.");
            }

            if (!dryRun) {
                QuestionRequest request = questionImportSupport.toQuestionRequest(payload, status);
                Question question = questionMapper.toEntity(request);
                question.setImportBatch(batch);
                question.setStatementHash(statementHash);
                question.setImportedAt(OffsetDateTime.now());
                questionRepository.save(question);
                imported++;
            }

            if (!itemMessages.isEmpty()) {
                itemErrors.add(toItemError(index, payload, itemMessages));
            }
        }

        if (!dryRun && batch != null) {
            batch.setImportedItems(imported);
            batch.setSkippedItems(skippedDuplicates);
            batch.setFailedItems(errors);
            batch.setNeedsReviewItems(needsReview);
            batch.setFinishedAt(OffsetDateTime.now());
            batch.setErrorReport(itemErrors.isEmpty() ? null : serializeErrors(itemErrors));
            batch.setStatus(resolveBatchStatus(imported, errors, needsReview, skippedDuplicates));
            importBatchRepository.save(batch);
        }

        return new ImportReportResponse(
                batch == null ? null : batch.getId(),
                payloads.size(),
                imported,
                skippedDuplicates,
                errors,
                needsReview,
                invalid,
                dryRun,
                itemErrors
        );
    }

    private boolean isDuplicate(ImportQuestionPayload payload, String statementHash) {
        boolean sameExternalIdentity = payload.externalProvider() != null
                && !payload.externalProvider().isBlank()
                && payload.externalQuestionId() != null
                && !payload.externalQuestionId().isBlank()
                && questionRepository.existsByExternalProviderIgnoreCaseAndExternalQuestionIdIgnoreCase(
                payload.externalProvider(),
                payload.externalQuestionId()
        );
        boolean sameSourceIdentity = payload.sourceBookColor() != null && payload.sourceDay() != null
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

    private ImportBatch startBatch(List<ImportQuestionPayload> payloads) {
        ImportQuestionPayload first = payloads.getFirst();
        ImportBatch batch = new ImportBatch();
        batch.setSourceName(first.source());
        batch.setSourceUrl(first.sourceUrl());
        batch.setExam(first.sourceExam());
        batch.setYear(first.sourceYear());
        batch.setStatus(ImportBatchStatus.PROCESSING);
        batch.setStartedAt(OffsetDateTime.now());
        batch.setTotalItems(payloads.size());
        return importBatchRepository.save(batch);
    }

    private ImportBatchStatus resolveBatchStatus(int imported, int errors, int needsReview, int skippedDuplicates) {
        if (errors > 0 && imported == 0 && needsReview == 0) {
            return ImportBatchStatus.FAILED;
        }
        if (errors > 0 || needsReview > 0 || skippedDuplicates > 0) {
            return ImportBatchStatus.PARTIAL;
        }
        return ImportBatchStatus.COMPLETED;
    }

    private List<ImportQuestionPayload> readJson(MultipartFile file) {
        try {
            return objectMapper.readValue(file.getInputStream(), QUESTION_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Nao foi possivel ler o JSON de importacao.", exception);
        }
    }

    private List<ImportQuestionPayload> readCsv(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            List<ImportQuestionPayload> payloads = new ArrayList<>();
            for (CSVRecord row : parser) {
                payloads.add(new ImportQuestionPayload(
                        row.get("title"),
                        row.get("statement"),
                        optional(row, "statementHtml"),
                        optional(row, "imageUrl"),
                        row.get("subject"),
                        row.get("topic"),
                        optional(row, "subtopic"),
                        com.gabaritaplus.api.entity.enums.DifficultyLevel.valueOf(row.get("difficulty").trim().toUpperCase(Locale.ROOT)),
                        Integer.valueOf(row.get("year")),
                        row.get("exam"),
                        optional(row, "competency"),
                        optional(row, "ability"),
                        optional(row, "explanation"),
                        row.get("correctAlternative"),
                        row.get("source"),
                        row.get("sourceUrl"),
                        row.get("sourceExam"),
                        Integer.valueOf(row.get("sourceYear")),
                        Integer.valueOf(row.get("sourceQuestionNumber")),
                        optional(row, "sourceBookColor"),
                        optionalInteger(row, "sourceDay"),
                        optionalInteger(row, "sourcePage"),
                        optional(row, "officialSourceUrl"),
                        optional(row, "officialPdfUrl"),
                        optional(row, "officialAnswerKeyUrl"),
                        optionalInteger(row, "officialPage"),
                        optionalBoolean(row, "validatedAgainstOfficialSource"),
                        optionalOffsetDateTime(row, "validatedAt"),
                        optional(row, "externalProvider"),
                        optional(row, "externalProviderUrl"),
                        optional(row, "externalQuestionId"),
                        optional(row, "externalLicense"),
                        parseAssets(optional(row, "assets")),
                        parseAlternatives(row.get("alternatives"))
                ));
            }
            return payloads;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Nao foi possivel ler o CSV de importacao.", exception);
        }
    }

    private List<ImportAlternativePayload> parseAlternatives(String json) throws IOException {
        return objectMapper.readValue(json, ALTERNATIVE_LIST_TYPE);
    }

    private List<ImportQuestionAssetPayload> parseAssets(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, ASSET_LIST_TYPE);
    }

    private String optional(CSVRecord row, String header) {
        return row.isMapped(header) ? emptyToNull(row.get(header)) : null;
    }

    private Integer optionalInteger(CSVRecord row, String header) {
        String value = optional(row, header);
        return value == null ? null : Integer.valueOf(value);
    }

    private Boolean optionalBoolean(CSVRecord row, String header) {
        String value = optional(row, header);
        return value == null ? null : Boolean.valueOf(value);
    }

    private OffsetDateTime optionalOffsetDateTime(CSVRecord row, String header) {
        String value = optional(row, header);
        return value == null ? null : OffsetDateTime.parse(value);
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ImportItemErrorResponse toItemError(int index, ImportQuestionPayload payload, List<String> messages) {
        return new ImportItemErrorResponse(index, payload.title(), payload.sourceQuestionNumber(), List.copyOf(messages));
    }

    private String serializeErrors(List<ImportItemErrorResponse> errors) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errors);
        } catch (IOException exception) {
            return "[{\"error\":\"Falha ao serializar relatorio.\"}]";
        }
    }

    private ImportBatchResponse toBatchResponse(ImportBatch batch) {
        return new ImportBatchResponse(
                batch.getId(),
                batch.getSourceName(),
                batch.getSourceUrl(),
                batch.getExam(),
                batch.getYear(),
                batch.getStatus(),
                batch.getTotalItems(),
                batch.getImportedItems(),
                batch.getSkippedItems(),
                batch.getFailedItems(),
                batch.getNeedsReviewItems(),
                batch.getStartedAt(),
                batch.getFinishedAt(),
                batch.getErrorReport(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }
}
