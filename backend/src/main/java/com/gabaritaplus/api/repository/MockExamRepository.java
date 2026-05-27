package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {
    List<MockExam> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<MockExam> findByIdAndUserId(Long id, Long userId);
}
