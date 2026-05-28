package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

    List<Question> findByImportStatusIn(Collection<QuestionImportStatus> statuses);
}
