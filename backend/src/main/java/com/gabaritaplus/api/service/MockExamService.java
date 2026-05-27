package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.mockexam.FinishMockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamAnswerRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamAnswerResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamQuestionDetailResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamQuestionResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamRequest;
import com.gabaritaplus.api.dto.mockexam.MockExamResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamResultResponse;
import com.gabaritaplus.api.dto.mockexam.MockExamSubjectPerformanceResponse;
import com.gabaritaplus.api.entity.MockExam;
import com.gabaritaplus.api.entity.MockExamAnswer;
import com.gabaritaplus.api.entity.MockExamQuestion;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.exception.ResourceNotFoundException;
import com.gabaritaplus.api.mapper.MockExamMapper;
import com.gabaritaplus.api.repository.MockExamRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockExamService {

    private static final int DEFAULT_AUTO_QUESTION_COUNT = 10;

    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;
    private final MockExamMapper mockExamMapper;
    private final AuthenticatedUserService authenticatedUserService;
    private final QuestionService questionService;

    @Transactional
    public MockExamResponse create(MockExamRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        List<Question> questions = resolveQuestions(request);

        MockExam mockExam = new MockExam();
        mockExam.setTitle(request.title().trim());
        mockExam.setDurationMinutes(request.durationMinutes());
        mockExam.setUser(user);

        List<MockExamQuestion> mockExamQuestions = new ArrayList<>();
        int order = 1;
        for (Question question : questions) {
            MockExamQuestion mockExamQuestion = new MockExamQuestion();
            mockExamQuestion.setMockExam(mockExam);
            mockExamQuestion.setQuestion(question);
            mockExamQuestion.setQuestionOrder(order++);
            mockExamQuestions.add(mockExamQuestion);
        }

        mockExam.setQuestions(mockExamQuestions);
        MockExam savedMockExam = mockExamRepository.save(mockExam);
        log.info(
                "Simulado criado. userId={}, mockExamId={}, questionCount={}",
                user.getId(),
                savedMockExam.getId(),
                savedMockExam.getQuestions().size()
        );
        return toResponse(savedMockExam);
    }

    public List<MockExamResponse> listMine() {
        User user = authenticatedUserService.getCurrentUser();
        return mockExamRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MockExamResponse getById(Long id) {
        return toResponse(getOwnedMockExam(id));
    }

    public List<MockExamQuestionDetailResponse> getQuestions(Long id) {
        MockExam mockExam = getOwnedMockExam(id);
        return getOrderedQuestions(mockExam)
                .stream()
                .map(question -> mockExamMapper.toQuestionDetailResponse(
                        question,
                        getAnswerMap(mockExam).get(question.getQuestion().getId()),
                        mockExam.isFinished()
                ))
                .toList();
    }

    @Transactional
    public MockExamAnswerResponse answerQuestion(Long id, MockExamAnswerRequest request) {
        MockExam mockExam = getOwnedMockExam(id);
        if (mockExam.isFinished()) {
            throw new IllegalArgumentException("Este simulado ja foi finalizado.");
        }

        MockExamQuestion mockExamQuestion = findMockExamQuestion(mockExam, request.questionId());
        String chosenAlternative = normalizeAlternative(request.chosenAlternative());
        validateAlternative(mockExamQuestion.getQuestion(), chosenAlternative);

        MockExamAnswer answer = getAnswerMap(mockExam).get(mockExamQuestion.getQuestion().getId());
        if (answer == null) {
            answer = new MockExamAnswer();
            answer.setMockExam(mockExam);
            answer.setQuestion(mockExamQuestion.getQuestion());
            mockExam.getAnswers().add(answer);
        }

        answer.setChosenAlternative(chosenAlternative);
        answer.setTimeSpentSeconds(request.timeSpentSeconds());
        answer.setCorrect(mockExamQuestion.getQuestion().getCorrectAlternative().equalsIgnoreCase(chosenAlternative));

        MockExam savedMockExam = mockExamRepository.save(mockExam);
        int answeredCount = savedMockExam.getAnswers().size();
        int unansweredCount = Math.max(savedMockExam.getQuestions().size() - answeredCount, 0);

        log.info(
                "Resposta de simulado registrada. userId={}, mockExamId={}, questionId={}",
                mockExam.getUser().getId(),
                savedMockExam.getId(),
                request.questionId()
        );

        return new MockExamAnswerResponse(
                mockExamQuestion.getQuestion().getId(),
                mockExamQuestion.getQuestionOrder(),
                chosenAlternative,
                answeredCount,
                unansweredCount,
                answer.getUpdatedAt()
        );
    }

    @Transactional
    public MockExamResultResponse finish(Long id, FinishMockExamRequest request) {
        MockExam mockExam = getOwnedMockExam(id);
        if (mockExam.isFinished()) {
            return toResultResponse(mockExam);
        }

        mockExam.setFinished(true);
        mockExam.setFinishedAt(OffsetDateTime.now());
        mockExam.setTimeSpentSeconds(resolveTimeSpentSeconds(mockExam, request));
        mockExam.setFinalScore(calculateFinalScore(mockExam));

        for (MockExamAnswer answer : mockExam.getAnswers()) {
            questionService.recordUserAnswer(
                    mockExam.getUser(),
                    answer.getQuestion(),
                    answer.getChosenAlternative(),
                    answer.getTimeSpentSeconds()
            );
        }

        MockExam savedMockExam = mockExamRepository.save(mockExam);
        log.info(
                "Simulado finalizado. userId={}, mockExamId={}, answeredCount={}, finalScore={}",
                mockExam.getUser().getId(),
                savedMockExam.getId(),
                savedMockExam.getAnswers().size(),
                savedMockExam.getFinalScore()
        );
        return toResultResponse(savedMockExam);
    }

    public MockExamResultResponse getResult(Long id) {
        MockExam mockExam = getOwnedMockExam(id);
        if (!mockExam.isFinished()) {
            throw new IllegalArgumentException("Finalize o simulado antes de consultar o resultado.");
        }
        return toResultResponse(mockExam);
    }

    private List<Question> resolveQuestions(MockExamRequest request) {
        List<Long> requestedQuestionIds = request.questionIds() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.questionIds()));

        if (!requestedQuestionIds.isEmpty()) {
            List<Question> questions = questionRepository.findAllById(requestedQuestionIds);
            if (questions.size() != requestedQuestionIds.size()) {
                throw new ResourceNotFoundException("Uma ou mais questoes do simulado nao foram encontradas.");
            }

            Map<Long, Question> questionsById = questions.stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
            return requestedQuestionIds.stream()
                    .map(questionsById::get)
                    .filter(Objects::nonNull)
                    .toList();
        }

        int questionCount = request.questionCount() != null
                ? request.questionCount()
                : DEFAULT_AUTO_QUESTION_COUNT;

        List<Question> autoSelectedQuestions = questionRepository.findAll(
                        PageRequest.of(0, questionCount, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .getContent();

        if (autoSelectedQuestions.isEmpty()) {
            throw new IllegalArgumentException("Nao ha questoes suficientes para criar o simulado.");
        }

        return autoSelectedQuestions;
    }

    private MockExamResponse toResponse(MockExam mockExam) {
        List<MockExamQuestion> orderedQuestions = getOrderedQuestions(mockExam);
        Map<Long, MockExamAnswer> answerMap = getAnswerMap(mockExam);
        int questionCount = orderedQuestions.size();
        int answeredCount = answerMap.size();
        int correctCount = (int) answerMap.values().stream().filter(MockExamAnswer::isCorrect).count();
        int incorrectCount = answeredCount - correctCount;

        List<MockExamQuestionResponse> questionResponses = orderedQuestions.stream()
                .map(question -> mockExamMapper.toQuestionResponse(
                        question,
                        answerMap.get(question.getQuestion().getId())
                ))
                .toList();

        return new MockExamResponse(
                mockExam.getId(),
                mockExam.getTitle(),
                mockExam.getDurationMinutes(),
                mockExam.isFinished(),
                mockExam.getFinalScore(),
                questionCount,
                answeredCount,
                correctCount,
                incorrectCount,
                Math.max(questionCount - answeredCount, 0),
                mockExam.getTimeSpentSeconds(),
                mockExam.getFinishedAt(),
                questionResponses,
                mockExam.getCreatedAt()
        );
    }

    private MockExamResultResponse toResultResponse(MockExam mockExam) {
        Map<Long, MockExamAnswer> answerMap = getAnswerMap(mockExam);
        List<MockExamQuestion> orderedQuestions = getOrderedQuestions(mockExam);
        int questionCount = orderedQuestions.size();
        int correctAnswers = (int) answerMap.values().stream().filter(MockExamAnswer::isCorrect).count();
        int incorrectAnswers = answerMap.size() - correctAnswers;
        int unansweredQuestions = Math.max(questionCount - answerMap.size(), 0);

        List<MockExamQuestionDetailResponse> questionDetails = orderedQuestions.stream()
                .map(question -> mockExamMapper.toQuestionDetailResponse(
                        question,
                        answerMap.get(question.getQuestion().getId()),
                        true
                ))
                .toList();

        return new MockExamResultResponse(
                mockExam.getId(),
                mockExam.getTitle(),
                mockExam.isFinished(),
                mockExam.getFinalScore(),
                questionCount,
                correctAnswers,
                incorrectAnswers,
                unansweredQuestions,
                mockExam.getTimeSpentSeconds(),
                mockExam.getFinishedAt(),
                buildPerformanceBySubject(orderedQuestions, answerMap),
                questionDetails
        );
    }

    private List<MockExamSubjectPerformanceResponse> buildPerformanceBySubject(
            List<MockExamQuestion> orderedQuestions,
            Map<Long, MockExamAnswer> answerMap
    ) {
        Map<String, List<MockExamQuestion>> questionsBySubject = orderedQuestions.stream()
                .collect(Collectors.groupingBy(
                        question -> question.getQuestion().getSubject(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        return questionsBySubject.entrySet().stream()
                .map(entry -> {
                    int totalQuestions = entry.getValue().size();
                    int correctAnswers = (int) entry.getValue().stream()
                            .map(question -> answerMap.get(question.getQuestion().getId()))
                            .filter(Objects::nonNull)
                            .filter(MockExamAnswer::isCorrect)
                            .count();
                    int incorrectAnswers = (int) entry.getValue().stream()
                            .map(question -> answerMap.get(question.getQuestion().getId()))
                            .filter(Objects::nonNull)
                            .filter(answer -> !answer.isCorrect())
                            .count();
                    BigDecimal accuracy = totalQuestions == 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(correctAnswers * 100.0 / totalQuestions)
                            .setScale(1, RoundingMode.HALF_UP);

                    return new MockExamSubjectPerformanceResponse(
                            entry.getKey(),
                            totalQuestions,
                            correctAnswers,
                            incorrectAnswers,
                            accuracy
                    );
                })
                .toList();
    }

    private BigDecimal calculateFinalScore(MockExam mockExam) {
        int totalQuestions = mockExam.getQuestions().size();
        if (totalQuestions == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long correctAnswers = mockExam.getAnswers().stream().filter(MockExamAnswer::isCorrect).count();
        return BigDecimal.valueOf(correctAnswers * 100.0 / totalQuestions).setScale(1, RoundingMode.HALF_UP);
    }

    private Long resolveTimeSpentSeconds(MockExam mockExam, FinishMockExamRequest request) {
        if (request != null && request.timeSpentSeconds() != null) {
            return request.timeSpentSeconds();
        }

        return mockExam.getAnswers().stream()
                .map(MockExamAnswer::getTimeSpentSeconds)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
    }

    private Map<Long, MockExamAnswer> getAnswerMap(MockExam mockExam) {
        return mockExam.getAnswers().stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
    }

    private List<MockExamQuestion> getOrderedQuestions(MockExam mockExam) {
        return mockExam.getQuestions().stream()
                .sorted(Comparator.comparing(MockExamQuestion::getQuestionOrder))
                .toList();
    }

    private MockExamQuestion findMockExamQuestion(MockExam mockExam, Long questionId) {
        return mockExam.getQuestions().stream()
                .filter(question -> question.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Questao nao encontrada neste simulado."));
    }

    private void validateAlternative(Question question, String chosenAlternative) {
        Set<String> validAlternatives = question.getAlternatives().stream()
                .map(alternative -> alternative.getLetter().toUpperCase())
                .collect(Collectors.toSet());

        if (!validAlternatives.contains(chosenAlternative)) {
            throw new IllegalArgumentException("Alternativa invalida para esta questao.");
        }
    }

    private String normalizeAlternative(String chosenAlternative) {
        return chosenAlternative.trim().toUpperCase();
    }

    private MockExam getOwnedMockExam(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();
        return mockExamRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Simulado nao encontrado."));
    }
}
