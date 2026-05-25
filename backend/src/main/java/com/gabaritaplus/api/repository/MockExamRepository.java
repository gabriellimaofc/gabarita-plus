package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {
    List<MockExam> findByUserIdOrderByCreatedAtDesc(Long userId);
}
