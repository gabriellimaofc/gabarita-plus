package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.FavoriteQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteQuestionRepository extends JpaRepository<FavoriteQuestion, Long> {
    Optional<FavoriteQuestion> findByUserIdAndQuestionId(Long userId, Long questionId);
    List<FavoriteQuestion> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("""
            select fq.question.id
            from FavoriteQuestion fq
            where fq.user.id = :userId
            """)
    List<Long> findFavoriteQuestionIdsByUserId(Long userId);
}
