package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.dto.importer.ImportAlternativePayload;
import com.gabaritaplus.api.dto.importer.ImportQuestionAssetPayload;
import com.gabaritaplus.api.dto.importer.ImportQuestionPayload;
import com.gabaritaplus.api.dto.question.AlternativeRequest;
import com.gabaritaplus.api.dto.question.QuestionAssetRequest;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class QuestionImportSupport {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
    private static final List<String> VISUAL_REFERENCES = List.of(
            "observe o grafico",
            "a figura mostra",
            "na imagem",
            "o mapa",
            "a tabela",
            "o esquema",
            "o desenho",
            "a charge",
            "o infografico",
            "a tirinha",
            "o diagrama",
            "a seguir",
            "a imagem",
            "a ilustracao"
    );
    private static final List<String> BROKEN_TEXT_MARKERS = List.of("Ã§", "Ã£", "Ã©", "Ãª", "Ã³", "Ãº", "â€œ", "â€", "â€“");
    private static final Set<String> REQUIRED_ALTERNATIVES = Set.of("A", "B", "C", "D", "E");

    public String generateStatementHash(String statement, String statementHtml) {
        String baseText = normalizeForHash(statementHtml != null && !statementHtml.isBlank() ? stripHtml(statementHtml) : statement);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(baseText.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }

    public List<String> validateImportPayload(ImportQuestionPayload payload) {
        return validateImportPayload(payload, ImportValidationMode.OFFICIAL);
    }

    public List<String> validateImportPayload(ImportQuestionPayload payload, ImportValidationMode mode) {
        List<String> errors = new ArrayList<>();
        if (mode == ImportValidationMode.OFFICIAL
                && !"INEP".equalsIgnoreCase(Objects.toString(payload.source(), "").trim())) {
            errors.add("A fonte precisa ser INEP.");
        }
        if (mode == ImportValidationMode.EXTERNAL_AUXILIARY
                && Objects.toString(payload.externalProvider(), "").isBlank()) {
            errors.add("Questoes de fonte auxiliar precisam informar o provedor externo.");
        }
        if (mode == ImportValidationMode.OFFICIAL && Objects.toString(payload.sourceBookColor(), "").isBlank()) {
            errors.add("A cor do caderno de origem e obrigatoria.");
        }
        if (mode == ImportValidationMode.OFFICIAL && payload.sourceDay() == null) {
            errors.add("O dia da prova de origem e obrigatorio.");
        }

        List<ImportAlternativePayload> alternatives = normalizeAlternativePayloads(payload.alternatives());
        if (alternatives.size() != 5) {
            errors.add("A questao precisa ter exatamente 5 alternativas.");
        }

        Set<String> letters = alternatives.stream()
                .map(item -> normalizeLetter(item.letter()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!letters.equals(REQUIRED_ALTERNATIVES)) {
            errors.add("As alternativas precisam conter exatamente as letras A, B, C, D e E.");
        }

        String correctAlternative = normalizeLetter(payload.correctAlternative());
        if (!REQUIRED_ALTERNATIVES.contains(correctAlternative)) {
            errors.add("O gabarito precisa estar entre A e E.");
        }

        if (alternatives.stream().anyMatch(item -> item.text() == null || item.text().isBlank())) {
            errors.add("Todas as alternativas precisam ter texto.");
        }

        return errors;
    }

    public QuestionImportStatus determineStatus(ImportQuestionPayload payload, List<String> validationErrors) {
        return determineStatus(payload, validationErrors, ImportValidationMode.OFFICIAL);
    }

    public QuestionImportStatus determineStatus(
            ImportQuestionPayload payload,
            List<String> validationErrors,
            ImportValidationMode mode
    ) {
        if (!validationErrors.isEmpty()) {
            return QuestionImportStatus.INVALID;
        }
        if (mode == ImportValidationMode.EXTERNAL_AUXILIARY
                && !Boolean.TRUE.equals(payload.validatedAgainstOfficialSource())) {
            return QuestionImportStatus.NEEDS_REVIEW;
        }
        if (hasBrokenText(payload.statement(), payload.statementHtml())) {
            return QuestionImportStatus.NEEDS_REVIEW;
        }
        if (referencesVisualContentWithoutAssets(payload)) {
            return QuestionImportStatus.NEEDS_REVIEW;
        }
        return QuestionImportStatus.VALIDATED;
    }

    public boolean referencesVisualContentWithoutAssets(ImportQuestionPayload payload) {
        String combined = normalizeForHash(payload.statement() + " " + Objects.toString(payload.statementHtml(), ""));
        boolean mentionsVisual = VISUAL_REFERENCES.stream().anyMatch(combined::contains);
        boolean hasQuestionAssets = payload.assets() != null && !payload.assets().isEmpty();
        return mentionsVisual && !hasQuestionAssets;
    }

    public boolean hasBrokenText(String statement, String statementHtml) {
        String combined = Objects.toString(statement, "") + " " + Objects.toString(statementHtml, "");
        return BROKEN_TEXT_MARKERS.stream().anyMatch(combined::contains);
    }

    public QuestionRequest toQuestionRequest(ImportQuestionPayload payload, QuestionImportStatus status) {
        List<ImportAlternativePayload> normalizedAlternatives = normalizeAlternativePayloads(payload.alternatives());
        List<AlternativeRequest> alternatives = normalizedAlternatives.stream()
                .map(item -> new AlternativeRequest(
                        normalizeLetter(item.letter()),
                        item.text().trim(),
                        normalizeOptionalText(item.html()),
                        mapAssets(item.assets()),
                        normalizeLetter(item.letter()).equals(normalizeLetter(payload.correctAlternative()))
                ))
                .sorted(Comparator.comparing(AlternativeRequest::letter))
                .toList();

        return new QuestionRequest(
                payload.title().trim(),
                payload.statement().trim(),
                normalizeOptionalText(payload.statementHtml()),
                normalizeOptionalText(payload.imageUrl()),
                payload.subject().trim(),
                payload.topic().trim(),
                normalizeOptionalText(payload.subtopic()),
                payload.difficulty(),
                payload.year(),
                payload.exam().trim(),
                normalizeOptionalText(payload.competency()),
                normalizeOptionalText(payload.ability()),
                normalizeOptionalText(payload.explanation()),
                normalizeLetter(payload.correctAlternative()),
                payload.source().trim(),
                payload.sourceUrl().trim(),
                payload.sourceExam().trim(),
                payload.sourceYear(),
                payload.sourceQuestionNumber(),
                normalizeOptionalText(payload.sourceBookColor()),
                payload.sourceDay(),
                payload.sourcePage(),
                normalizeOptionalText(payload.officialSourceUrl()),
                normalizeOptionalText(payload.officialPdfUrl()),
                normalizeOptionalText(payload.officialAnswerKeyUrl()),
                payload.officialPage(),
                payload.validatedAgainstOfficialSource(),
                payload.validatedAt(),
                normalizeOptionalText(payload.externalProvider()),
                normalizeOptionalText(payload.externalProviderUrl()),
                normalizeOptionalText(payload.externalQuestionId()),
                normalizeOptionalText(payload.externalLicense()),
                status,
                mapAssets(payload.assets()),
                alternatives
        );
    }

    private List<ImportAlternativePayload> normalizeAlternativePayloads(List<ImportAlternativePayload> alternatives) {
        if (alternatives == null) {
            return List.of();
        }
        return alternatives.stream()
                .map(item -> new ImportAlternativePayload(
                        normalizeLetter(item.letter()),
                        item.text() == null ? null : item.text().trim(),
                        normalizeOptionalText(item.html()),
                        item.assets()
                ))
                .sorted(Comparator.comparing(ImportAlternativePayload::letter, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<QuestionAssetRequest> mapAssets(List<ImportQuestionAssetPayload> assets) {
        if (assets == null) {
            return List.of();
        }
        return assets.stream()
                .map(item -> new QuestionAssetRequest(
                        item.type(),
                        normalizeOptionalText(item.url()),
                        normalizeOptionalText(item.storagePath()),
                        normalizeOptionalText(item.originalFileName()),
                        item.sourcePage(),
                        item.cropX(),
                        item.cropY(),
                        item.cropWidth(),
                        item.cropHeight(),
                        normalizeOptionalText(item.altText()),
                        normalizeOptionalText(item.caption()),
                        normalizeOptionalText(item.checksum())
                ))
                .toList();
    }

    private String normalizeLetter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeForHash(String value) {
        String base = stripHtml(Objects.toString(value, ""));
        return Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripHtml(String value) {
        return HTML_TAGS.matcher(Objects.toString(value, "")).replaceAll(" ");
    }
}
