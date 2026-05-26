package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.dashboard.DashboardResponse;
import com.gabaritaplus.api.dto.dashboard.PerformanceByDimensionResponse;
import com.gabaritaplus.api.dto.dashboard.WeeklyProgressResponse;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserAnswerRepository userAnswerRepository;

    public DashboardResponse getDashboard() {
        User user = authenticatedUserService.getCurrentUser();
        long totalAnswered = userAnswerRepository.countByUserId(user.getId());
        long totalCorrect = userAnswerRepository.countByUserIdAndCorrectTrue(user.getId());
        double accuracyRate = totalAnswered == 0 ? 0.0 : (totalCorrect * 100.0) / totalAnswered;
        double averageTime = userAnswerRepository.findAverageTimeSpentByUserId(user.getId()).orElse(0.0);

        return new DashboardResponse(
                accuracyRate,
                totalAnswered,
                averageTime,
                userAnswerRepository.summarizeBySubject(user.getId()).stream().map(this::toPerformance).toList(),
                userAnswerRepository.summarizeByTopic(user.getId()).stream().map(this::toPerformance).toList(),
                userAnswerRepository.summarizeWeeklyProgress(user.getId()).stream().map(this::toWeeklyProgress).toList()
        );
    }

    private PerformanceByDimensionResponse toPerformance(Object[] values) {
        String name = (String) values[0];
        long total = ((Number) values[1]).longValue();
        long correct = values[2] == null ? 0L : ((Number) values[2]).longValue();
        double accuracy = total == 0 ? 0.0 : (correct * 100.0) / total;
        return new PerformanceByDimensionResponse(name, total, correct, accuracy);
    }

    private WeeklyProgressResponse toWeeklyProgress(Object[] values) {
        String week = (String) values[0];
        long total = ((Number) values[1]).longValue();
        long correct = values[2] == null ? 0L : ((Number) values[2]).longValue();
        double accuracy = total == 0 ? 0.0 : (correct * 100.0) / total;
        return new WeeklyProgressResponse(week, total, correct, accuracy);
    }
}
