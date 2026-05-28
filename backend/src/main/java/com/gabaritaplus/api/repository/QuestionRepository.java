package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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

    @Query("select q.importBatch.id from Question q where q.id = :id")
    Long findImportBatchIdByQuestionId(@Param("id") Long id);
}
