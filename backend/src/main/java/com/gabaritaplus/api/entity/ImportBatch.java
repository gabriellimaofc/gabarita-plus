package com.gabaritaplus.api.entity;

import com.gabaritaplus.api.entity.enums.ImportBatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_batches")
public class ImportBatch extends BaseEntity {

    @Column(name = "source_name", nullable = false, length = 120)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(nullable = false, length = 120)
    private String exam;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportBatchStatus status;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems = 0;

    @Column(name = "imported_items", nullable = false)
    private Integer importedItems = 0;

    @Column(name = "skipped_items", nullable = false)
    private Integer skippedItems = 0;

    @Column(name = "failed_items", nullable = false)
    private Integer failedItems = 0;

    @Column(name = "needs_review_items", nullable = false)
    private Integer needsReviewItems = 0;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "error_report", columnDefinition = "TEXT")
    private String errorReport;
}
