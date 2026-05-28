package com.gabaritaplus.api.dto.importer;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record ImportQuestionPayload(
        @NotBlank(message = "Titulo e obrigatorio.")
        @Size(max = 180)
        String title,
        @NotBlank(message = "Enunciado e obrigatorio.")
        String statement,
        String statementHtml,
        @Size(max = 500)
        String imageUrl,
        @NotBlank(message = "Materia e obrigatoria.")
        @Size(max = 80)
        String subject,
        @NotBlank(message = "Tema e obrigatorio.")
        @Size(max = 120)
        String topic,
        @Size(max = 120)
        String subtopic,
        @NotNull(message = "Dificuldade e obrigatoria.")
        DifficultyLevel difficulty,
        @NotNull(message = "Ano e obrigatorio.")
        @Min(2000)
        @Max(2100)
        Integer year,
        @NotBlank(message = "Prova e obrigatoria.")
        @Size(max = 120)
        String exam,
        @Size(max = 120)
        String competency,
        @Size(max = 120)
        String ability,
        String explanation,
        @NotBlank(message = "Gabarito e obrigatorio.")
        @Size(min = 1, max = 1)
        String correctAlternative,
        @NotBlank(message = "Fonte e obrigatoria.")
        @Size(max = 80)
        String source,
        @NotBlank(message = "URL da fonte e obrigatoria.")
        @Size(max = 1000)
        String sourceUrl,
        @NotBlank(message = "Prova de origem e obrigatoria.")
        @Size(max = 120)
        String sourceExam,
        @NotNull(message = "Ano de origem e obrigatorio.")
        @Min(2000)
        @Max(2100)
        Integer sourceYear,
        @NotNull(message = "Numero oficial da questao e obrigatorio.")
        Integer sourceQuestionNumber,
        @Size(max = 40)
        String sourceBookColor,
        Integer sourceDay,
        Integer sourcePage,
        @Size(max = 1000)
        String officialSourceUrl,
        @Size(max = 1000)
        String officialPdfUrl,
        @Size(max = 1000)
        String officialAnswerKeyUrl,
        Integer officialPage,
        Boolean validatedAgainstOfficialSource,
        OffsetDateTime validatedAt,
        @Size(max = 120)
        String externalProvider,
        @Size(max = 1000)
        String externalProviderUrl,
        @Size(max = 255)
        String externalQuestionId,
        @Size(max = 255)
        String externalLicense,
        @Valid
        List<ImportQuestionAssetPayload> assets,
        @Valid
        @NotEmpty(message = "A questao deve possuir alternativas.")
        List<ImportAlternativePayload> alternatives
) {
}
