package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    long countByUserId(Long userId);

    long countByUserIdAndCorrectTrue(Long userId);

    long countByUserIdAndQuestionSubject(Long userId, String subject);

    long countByUserIdAndQuestionSubjectAndCorrectTrue(Long userId, String subject);

    long countByUserIdAndQuestionTopic(Long userId, String topic);

    long countByUserIdAndQuestionTopicAndCorrectTrue(Long userId, String topic);

    @Query("""
            select avg(ua.timeSpentSeconds)
            from UserAnswer ua
            where ua.user.id = :userId
            """)
    Optional<Double> findAverageTimeSpentByUserId(Long userId);

    List<UserAnswer> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserAnswer> findTopByUserIdAndQuestionIdOrderByAttemptNumberDesc(Long userId, Long questionId);

    List<UserAnswer> findByUserIdAndQuestionIdIn(Long userId, Collection<Long> questionIds);

    @Query("""
            select distinct ua.question.id
            from UserAnswer ua
            where ua.user.id = :userId
            """)
    List<Long> findAnsweredQuestionIdsByUserId(Long userId);

    @Query("""
            select distinct ua.question.id
            from UserAnswer ua
            where ua.user.id = :userId and ua.correct = false
            """)
    List<Long> findIncorrectQuestionIdsByUserId(Long userId);

    @Query("""
            select ua.question.subject, count(ua), sum(case when ua.correct = true then 1 else 0 end)
            from UserAnswer ua
            where ua.user.id = :userId
            group by ua.question.subject
            """)
    List<Object[]> summarizeBySubject(Long userId);

    @Query("""
            select ua.question.topic, count(ua), sum(case when ua.correct = true then 1 else 0 end)
            from UserAnswer ua
            where ua.user.id = :userId
            group by ua.question.topic
            """)
    List<Object[]> summarizeByTopic(Long userId);

    @Query("""
            select ua.question.id, max(ua.createdAt)
            from UserAnswer ua
            where ua.user.id = :userId
              and ua.correct = false
              and ua.question.id in :questionIds
            group by ua.question.id
            """)
    List<Object[]> findLatestIncorrectAnswerByQuestionIds(Long userId, Collection<Long> questionIds);

    @Query(value = """
            select to_char(created_at at time zone 'UTC', 'IYYY-IW') as week_label,
                   count(*) as total_answers,
                   sum(case when correct = true then 1 else 0 end) as correct_answers
            from user_answers
            where user_id = :userId
            group by week_label
            order by week_label
            """, nativeQuery = true)
    List<Object[]> summarizeWeeklyProgress(Long userId);
}
