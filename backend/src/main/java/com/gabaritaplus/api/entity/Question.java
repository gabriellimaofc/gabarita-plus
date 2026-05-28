package com.gabaritaplus.api.entity;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "statement_html", columnDefinition = "TEXT")
    private String statementHtml;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(length = 120)
    private String subtopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 120)
    private String exam;

    @Column(length = 120)
    private String competency;

    @Column(length = 120)
    private String ability;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "correct_alternative", nullable = false, length = 1)
    private String correctAlternative;

    @Column(nullable = false, length = 80)
    private String source = "PLATFORM";

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl = "internal://legacy-question";

    @Column(name = "source_exam", nullable = false, length = 120)
    private String sourceExam;

    @Column(name = "source_year", nullable = false)
    private Integer sourceYear;

    @Column(name = "source_question_number")
    private Integer sourceQuestionNumber;

    @Column(name = "source_book_color", length = 40)
    private String sourceBookColor;

    @Column(name = "source_day")
    private Integer sourceDay;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "official_source_url", length = 1000)
    private String officialSourceUrl;

    @Column(name = "official_pdf_url", length = 1000)
    private String officialPdfUrl;

    @Column(name = "official_answer_key_url", length = 1000)
    private String officialAnswerKeyUrl;

    @Column(name = "official_page")
    private Integer officialPage;

    @Column(name = "validated_against_official_source", nullable = false)
    private Boolean validatedAgainstOfficialSource = false;

    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;

    @Column(name = "external_provider", length = 120)
    private String externalProvider;

    @Column(name = "external_provider_url", length = 1000)
    private String externalProviderUrl;

    @Column(name = "external_question_id", length = 255)
    private String externalQuestionId;

    @Column(name = "external_license", length = 255)
    private String externalLicense;

    @Column(name = "imported_at")
    private OffsetDateTime importedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;

    @Column(name = "statement_hash", nullable = false, length = 128)
    private String statementHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 30)
    private QuestionImportStatus importStatus = QuestionImportStatus.PUBLISHED;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alternative> alternatives = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAsset> assets = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void applyImportDefaults() {
        if (sourceExam == null) {
            sourceExam = exam;
        }
        if (sourceYear == null) {
            sourceYear = year;
        }
        if (statementHash == null || statementHash.isBlank()) {
            statementHash = Integer.toHexString((title + "::" + statement).hashCode());
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            sourceUrl = "internal://legacy-question";
        }
        if (source == null || source.isBlank()) {
            source = "PLATFORM";
        }
        if (importStatus == null) {
            importStatus = QuestionImportStatus.PUBLISHED;
        }
        if (validatedAgainstOfficialSource == null) {
            validatedAgainstOfficialSource = false;
        }
        if (importedAt == null) {
            importedAt = OffsetDateTime.now();
        }
    }
}
