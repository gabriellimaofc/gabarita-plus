package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewSummaryResponse;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    boolean existsBySourceExamIgnoreCaseAndSourceYearAndSourceQuestionNumberAndSourceDayAndSourceBookColorIgnoreCase(
            String sourceExam,
            Integer sourceYear,
            Integer sourceQuestionNumber,
            Integer sourceDay,
            String sourceBookColor
    );

    boolean existsByStatementHashAndSourceExamIgnoreCaseAndSourceYear(
            String statementHash,
            String sourceExam,
            Integer sourceYear
    );

    boolean existsByExternalProviderIgnoreCaseAndExternalQuestionIdIgnoreCase(
            String externalProvider,
            String externalQuestionId
    );

    List<Question> findByImportStatusIn(Collection<QuestionImportStatus> statuses);

    Page<Question> findByImportStatusIn(Collection<QuestionImportStatus> statuses, Pageable pageable);

    @Query("""
            select new com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewSummaryResponse(
                q.id,
                q.title,
                q.source,
                q.sourceYear,
                q.sourceQuestionNumber,
                q.sourceBookColor,
                q.sourceDay,
                q.importStatus,
                q.validatedAgainstOfficialSource,
                q.externalProvider,
                batch.id,
                q.subject,
                q.difficulty,
                q.createdAt,
                q.importedAt,
                count(distinct alternative.id),
                count(distinct asset.id)
            )
            from Question q
            left join q.importBatch batch
            left join q.alternatives alternative
            left join q.assets asset
            where q.importStatus in :statuses
              and (:source is null or lower(q.source) = lower(:source))
              and (:year is null or q.sourceYear = :year)
            group by q.id, q.title, q.source, q.sourceYear, q.sourceQuestionNumber, q.sourceBookColor,
                     q.sourceDay, q.importStatus, q.validatedAgainstOfficialSource, q.externalProvider,
                     batch.id, q.subject, q.difficulty, q.createdAt, q.importedAt
            """)
    Page<AdminImportedQuestionReviewSummaryResponse> findReviewSummaries(
            @Param("statuses") Collection<QuestionImportStatus> statuses,
            @Param("source") String source,
            @Param("year") Integer year,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "importBatch",
            "alternatives",
            "alternatives.assets",
            "assets"
    })
    @Query("select q from Question q where q.id = :id")
    Optional<Question> findDetailedForAdminReviewById(@Param("id") Long id);
}
