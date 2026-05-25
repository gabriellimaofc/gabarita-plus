package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.ErrorNotebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErrorNotebookRepository extends JpaRepository<ErrorNotebook, Long> {
    Optional<ErrorNotebook> findByUserIdAndQuestionId(Long userId, Long questionId);
    List<ErrorNotebook> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
