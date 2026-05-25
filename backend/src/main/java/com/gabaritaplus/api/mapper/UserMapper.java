package com.gabaritaplus.api.mapper;

import com.gabaritaplus.api.dto.user.RecentAnswerResponse;
import com.gabaritaplus.api.dto.user.UserProfileResponse;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.UserAnswer;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getTargetCourse(),
                user.isActive(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(java.util.stream.Collectors.toSet()),
                user.getCreatedAt()
        );
    }

    public RecentAnswerResponse toRecentAnswerResponse(UserAnswer answer) {
        return new RecentAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getQuestion().getTitle(),
                answer.getQuestion().getSubject(),
                answer.isCorrect(),
                answer.getChosenAlternative(),
                answer.getTimeSpentSeconds(),
                answer.getAttemptNumber(),
                answer.getCreatedAt()
        );
    }
}
