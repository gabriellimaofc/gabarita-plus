package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.mockexam.FinishMockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamResponse;
import com.gabaritaplus.api.entity.MockExam;
import com.gabaritaplus.api.entity.MockExamQuestion;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.exception.ResourceNotFoundException;
import com.gabaritaplus.api.mapper.MockExamMapper;
import com.gabaritaplus.api.repository.MockExamRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockExamService {

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final MockExamMapper mockExamMapper;
    private final AuthenticatedUserService authenticatedUserService;

    @Transactional
    public MockExamResponse create(MockExamRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        MockExam mockExam = new MockExam();
        mockExam.setTitle(request.title());
        mockExam.setDurationMinutes(request.durationMinutes());
        mockExam.setUser(user);

        List<MockExamQuestion> mockExamQuestions = new ArrayList<>();
        int order = 1;
        for (Long questionId : request.questionIds()) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada para o simulado."));
            MockExamQuestion mockExamQuestion = new MockExamQuestion();
            mockExamQuestion.setMockExam(mockExam);
            mockExamQuestion.setQuestion(question);
            mockExamQuestion.setQuestionOrder(order++);
            mockExamQuestions.add(mockExamQuestion);
        }
        mockExam.setQuestions(mockExamQuestions);
        return toResponse(mockExamRepository.save(mockExam));
    }

    public List<MockExamResponse> listMine() {
        User user = authenticatedUserService.getCurrentUser();
        return mockExamRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MockExamResponse finish(Long id, FinishMockExamRequest request) {
        MockExam mockExam = getOwnedMockExam(id);
        mockExam.setFinished(true);
        mockExam.setFinalScore(request.finalScore());
        return toResponse(mockExamRepository.save(mockExam));
    }

    public MockExamResponse getById(Long id) {
        return toResponse(getOwnedMockExam(id));
    }

    private MockExamResponse toResponse(MockExam mockExam) {
        return new MockExamResponse(
                mockExam.getId(),
                mockExam.getTitle(),
                mockExam.getDurationMinutes(),
                mockExam.isFinished(),
                mockExam.getFinalScore(),
                mockExam.getQuestions().stream().map(mockExamMapper::toQuestionResponse).toList(),
                mockExam.getCreatedAt()
        );
    }

    private MockExam getOwnedMockExam(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();
        MockExam mockExam = mockExamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulado não encontrado."));
        if (!mockExam.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Simulado não encontrado.");
        }
        return mockExam;
    }
}
