package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.user.UpdateUserProfileRequest;
import com.gabaritaplus.api.dto.user.UserProfileResponse;
import com.gabaritaplus.api.dto.user.UserStatisticsResponse;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.mapper.UserMapper;
import com.gabaritaplus.api.repository.UserAnswerRepository;
import com.gabaritaplus.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserMapper userMapper;

    public UserProfileResponse getCurrentProfile() {
        return userMapper.toProfileResponse(authenticatedUserService.getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        user.setBio(request.bio());
        user.setTargetCourse(request.targetCourse());
        return userMapper.toProfileResponse(userRepository.save(user));
    }

    public UserStatisticsResponse getStatistics() {
        User user = authenticatedUserService.getCurrentUser();
        long totalAnswers = userAnswerRepository.countByUserId(user.getId());
        long totalCorrect = userAnswerRepository.countByUserIdAndCorrectTrue(user.getId());
        double accuracy = totalAnswers == 0 ? 0.0 : (totalCorrect * 100.0) / totalAnswers;
        double averageTime = userAnswerRepository.findAverageTimeSpentByUserId(user.getId()).orElse(0.0);

        return new UserStatisticsResponse(
                totalAnswers,
                totalCorrect,
                accuracy,
                averageTime,
                userAnswerRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId())
                        .stream()
                        .map(userMapper::toRecentAnswerResponse)
                        .toList()
        );
    }
}
