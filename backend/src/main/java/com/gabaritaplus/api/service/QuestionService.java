package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.question.ErrorNotebookFilterRequest;
import com.gabaritaplus.api.dto.question.ErrorNotebookResponse;
import com.gabaritaplus.api.dto.question.QuestionFilterRequest;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.dto.question.QuestionResponse;
import com.gabaritaplus.api.dto.question.UpdateErrorNotebookStatusRequest;
import com.gabaritaplus.api.dto.question.UserAnswerRequest;
import com.gabaritaplus.api.dto.question.UserAnswerResponse;
import com.gabaritaplus.api.entity.ErrorNotebook;
import com.gabaritaplus.api.entity.FavoriteQuestion;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.UserAnswer;
import com.gabaritaplus.api.entity.enums.MasteryStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.entity.enums.ReviewPriority;
import com.gabaritaplus.api.exception.ResourceNotFoundException;
import com.gabaritaplus.api.mapper.QuestionMapper;
import com.gabaritaplus.api.repository.ErrorNotebookRepository;
import com.gabaritaplus.api.repository.FavoriteQuestionRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.UserAnswerRepository;
import com.gabaritaplus.api.service.importer.QuestionImportSupport;
import com.gabaritaplus.api.specification.QuestionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final FavoriteQuestionRepository favoriteQuestionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final ErrorNotebookRepository errorNotebookRepository;
    private final QuestionMapper questionMapper;
    private final AuthenticatedUserService authenticatedUserService;
    private final QuestionImportSupport questionImportSupport;

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        Question question = questionMapper.toEntity(request);
        hydrateImportFields(question);
        Question savedQuestion = questionRepository.save(question);
        User currentUser = authenticatedUserService.getCurrentUser();
        return enrichResponse(savedQuestion, isFavorite(currentUser.getId(), savedQuestion.getId()), null);
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Question existing = getQuestionEntity(id);
        questionMapper.apply(existing, request);
        hydrateImportFields(existing);

        User currentUser = authenticatedUserService.getCurrentUser();
        Question savedQuestion = questionRepository.save(existing);
        return enrichResponse(savedQuestion, isFavorite(currentUser.getId(), savedQuestion.getId()), null);
    }

    public QuestionResponse getById(Long id) {
        User user = authenticatedUserService.getCurrentUser();
        Question question = getAccessibleQuestion(id);
        UserAnswer latestAnswer = userAnswerRepository
                .findTopByUserIdAndQuestionIdOrderByAttemptNumberDesc(user.getId(), id)
                .orElse(null);
        return enrichResponse(question, isFavorite(user.getId(), id), latestAnswer);
    }

    public Page<QuestionResponse> search(QuestionFilterRequest filter, Pageable pageable) {
        User user = authenticatedUserService.getCurrentUser();
        List<Long> answeredIds = userAnswerRepository.findAnsweredQuestionIdsByUserId(user.getId());
        List<Long> incorrectIds = userAnswerRepository.findIncorrectQuestionIdsByUserId(user.getId());
        Set<Long> favoriteIds = new HashSet<>(favoriteQuestionRepository.findFavoriteQuestionIdsByUserId(user.getId()));

        Page<Question> result = questionRepository.findAll(
                QuestionSpecification.byFilters(filter, answeredIds, incorrectIds, favoriteIds.stream().toList()),
                pageable
        );

        List<Long> questionIds = result.getContent().stream().map(Question::getId).toList();
        Map<Long, UserAnswer> latestAnswers = findLatestAnswers(user.getId(), questionIds);

        return result.map(question -> enrichResponse(
                question,
                favoriteIds.contains(question.getId()),
                latestAnswers.get(question.getId())
        ));
    }

    public Page<QuestionResponse> listReviewQuestions(Pageable pageable) {
        return questionRepository.findByImportStatusIn(reviewableStatuses(), pageable)
                .map(questionMapper::toResponse);
    }

    public QuestionResponse getReviewQuestion(Long id) {
        Question question = getQuestionEntity(id);
        if (!reviewableStatuses().contains(question.getImportStatus())) {
            throw new ResourceNotFoundException("Questao nao encontrada em revisao.");
        }
        return questionMapper.toResponse(question);
    }

    @Transactional
    public void delete(Long id) {
        Question question = getQuestionEntity(id);
        questionRepository.delete(question);
    }

    @Transactional
    public UserAnswerResponse answerQuestion(UserAnswerRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        Question question = getAccessibleQuestion(request.questionId());
        UserAnswer savedAnswer = recordUserAnswer(
                user,
                question,
                normalizeAlternative(request.chosenAlternative()),
                request.timeSpentSeconds()
        );
        return questionMapper.toAnswerResponse(savedAnswer);
    }

    @Transactional
    public UserAnswer recordUserAnswer(
            User user,
            Question question,
            String chosenAlternative,
            long timeSpentSeconds
    ) {
        validateAlternative(question, chosenAlternative);

        int attemptNumber = userAnswerRepository
                .findTopByUserIdAndQuestionIdOrderByAttemptNumberDesc(user.getId(), question.getId())
                .map(answer -> answer.getAttemptNumber() + 1)
                .orElse(1);
        boolean correct = question.getCorrectAlternative().equalsIgnoreCase(chosenAlternative);

        UserAnswer answer = new UserAnswer();
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setChosenAlternative(chosenAlternative);
        answer.setTimeSpentSeconds(timeSpentSeconds);
        answer.setCorrect(correct);
        answer.setAttemptNumber(attemptNumber);

        UserAnswer savedAnswer = userAnswerRepository.save(answer);
        updateErrorNotebook(user, question, correct);
        log.info(
                "Resposta registrada. userId={}, questionId={}, correct={}, attemptNumber={}",
                user.getId(),
                question.getId(),
                correct,
                attemptNumber
        );
        return savedAnswer;
    }

    @Transactional
    public QuestionResponse toggleFavorite(Long questionId) {
        User user = authenticatedUserService.getCurrentUser();
        Question question = getAccessibleQuestion(questionId);
        favoriteQuestionRepository.findByUserIdAndQuestionId(user.getId(), questionId)
                .ifPresentOrElse(favoriteQuestionRepository::delete, () -> {
                    FavoriteQuestion favoriteQuestion = new FavoriteQuestion();
                    favoriteQuestion.setUser(user);
                    favoriteQuestion.setQuestion(question);
                    favoriteQuestionRepository.save(favoriteQuestion);
                });

        UserAnswer latestAnswer = userAnswerRepository
                .findTopByUserIdAndQuestionIdOrderByAttemptNumberDesc(user.getId(), questionId)
                .orElse(null);
        return enrichResponse(question, isFavorite(user.getId(), questionId), latestAnswer);
    }

    public List<ErrorNotebookResponse> getErrorNotebook() {
        return getErrorNotebook(new ErrorNotebookFilterRequest(null, null, null, null, null));
    }

    public List<ErrorNotebookResponse> getErrorNotebook(ErrorNotebookFilterRequest filter) {
        User user = authenticatedUserService.getCurrentUser();
        List<ErrorNotebook> notebooks = errorNotebookRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        Map<Long, LocalDate> latestIncorrectDates = findLatestIncorrectDates(
                user.getId(),
                notebooks.stream().map(notebook -> notebook.getQuestion().getId()).toList()
        );

        return notebooks.stream()
                .filter(notebook -> notebook.getQuestion().getImportStatus() == QuestionImportStatus.PUBLISHED)
                .map(notebook -> {
                    LocalDate lastErrorAt = latestIncorrectDates.get(notebook.getQuestion().getId());
                    ReviewPriority priority = determinePriority(notebook, lastErrorAt);
                    return questionMapper.toErrorNotebookResponse(notebook, lastErrorAt, priority);
                })
                .filter(entry -> matchesFilter(entry, filter))
                .sorted(buildNotebookComparator())
                .toList();
    }

    @Transactional
    public ErrorNotebookResponse updateErrorNotebookStatus(Long questionId, UpdateErrorNotebookStatusRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        ErrorNotebook notebook = errorNotebookRepository.findByUserIdAndQuestionId(user.getId(), questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Questao nao encontrada no caderno de erros."));

        notebook.setMasteryStatus(request.masteryStatus());
        notebook.setLastReviewedAt(LocalDate.now());
        notebook.setNextReviewAt(calculateNextReviewAt(request.masteryStatus()));

        ErrorNotebook savedNotebook = errorNotebookRepository.save(notebook);
        LocalDate lastErrorAt = findLatestIncorrectDates(user.getId(), List.of(questionId)).get(questionId);
        ReviewPriority priority = determinePriority(savedNotebook, lastErrorAt);
        return questionMapper.toErrorNotebookResponse(savedNotebook, lastErrorAt, priority);
    }

    public Question getPublishedQuestionEntity(Long id) {
        return getAccessibleQuestion(id);
    }

    private void hydrateImportFields(Question question) {
        if (question.getSourceExam() == null) {
            question.setSourceExam(question.getExam());
        }
        if (question.getSourceYear() == null) {
            question.setSourceYear(question.getYear());
        }
        if (question.getImportedAt() == null) {
            question.setImportedAt(OffsetDateTime.now());
        }
        question.setStatementHash(questionImportSupport.generateStatementHash(question.getStatement(), question.getStatementHtml()));
    }

    private boolean isFavorite(Long userId, Long questionId) {
        return favoriteQuestionRepository.findByUserIdAndQuestionId(userId, questionId).isPresent();
    }

    private Map<Long, UserAnswer> findLatestAnswers(Long userId, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, UserAnswer> latestAnswers = new HashMap<>();
        for (UserAnswer answer : userAnswerRepository.findByUserIdAndQuestionIdIn(userId, questionIds)) {
            latestAnswers.merge(
                    answer.getQuestion().getId(),
                    answer,
                    (current, candidate) -> current.getAttemptNumber() >= candidate.getAttemptNumber() ? current : candidate
            );
        }
        return latestAnswers;
    }

    private Map<Long, LocalDate> findLatestIncorrectDates(Long userId, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        return userAnswerRepository.findLatestIncorrectAnswerByQuestionIds(userId, questionIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((OffsetDateTime) row[1]).toLocalDate()
                ));
    }

    private boolean matchesFilter(ErrorNotebookResponse entry, ErrorNotebookFilterRequest filter) {
        if (filter == null) {
            return true;
        }

        return matchesText(entry.subject(), filter.subject())
                && matchesText(entry.topic(), filter.topic())
                && (filter.difficulty() == null || filter.difficulty() == entry.difficulty())
                && (filter.masteryStatus() == null || filter.masteryStatus() == entry.masteryStatus())
                && (filter.priority() == null || filter.priority() == entry.priority());
    }

    private boolean matchesText(String value, String filterValue) {
        if (filterValue == null || filterValue.isBlank()) {
            return true;
        }

        return value != null && value.toLowerCase().contains(filterValue.trim().toLowerCase());
    }

    private Comparator<ErrorNotebookResponse> buildNotebookComparator() {
        return Comparator
                .comparing((ErrorNotebookResponse entry) -> priorityRank(entry.priority()))
                .thenComparing(entry -> entry.nextReviewAt() == null ? LocalDate.MAX : entry.nextReviewAt())
                .thenComparing(ErrorNotebookResponse::updatedAt, Comparator.reverseOrder());
    }

    private int priorityRank(ReviewPriority priority) {
        if (priority == ReviewPriority.HIGH) {
            return 0;
        }
        if (priority == ReviewPriority.MEDIUM) {
            return 1;
        }
        return 2;
    }

    private ReviewPriority determinePriority(ErrorNotebook notebook, LocalDate lastErrorAt) {
        if (notebook.getMasteryStatus() == MasteryStatus.MASTERED) {
            return ReviewPriority.LOW;
        }

        LocalDate today = LocalDate.now();
        if ((notebook.getNextReviewAt() != null && !notebook.getNextReviewAt().isAfter(today))
                || notebook.getErrorCount() >= 3
                || notebook.getMasteryStatus() == MasteryStatus.LEARNING
                || (lastErrorAt != null && !lastErrorAt.isBefore(today.minusDays(2)))) {
            return ReviewPriority.HIGH;
        }

        if (notebook.getMasteryStatus() == MasteryStatus.REVIEW || notebook.getErrorCount() == 2) {
            return ReviewPriority.MEDIUM;
        }

        return ReviewPriority.LOW;
    }

    private LocalDate calculateNextReviewAt(MasteryStatus masteryStatus) {
        return switch (masteryStatus) {
            case MASTERED -> null;
            case REVIEW -> LocalDate.now().plusDays(7);
            case LEARNING -> LocalDate.now().plusDays(2);
            case NEW -> LocalDate.now().plusDays(1);
        };
    }

    private Question getQuestionEntity(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questao nao encontrada."));
    }

    private Question getAccessibleQuestion(Long id) {
        Question question = getQuestionEntity(id);
        if (question.getImportStatus() != QuestionImportStatus.PUBLISHED && !currentUserIsAdmin()) {
            throw new ResourceNotFoundException("Questao nao encontrada.");
        }
        return question;
    }

    private boolean currentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private QuestionResponse enrichResponse(Question question, boolean favorite, UserAnswer latestAnswer) {
        QuestionResponse base = questionMapper.toResponse(question);
        boolean answered = latestAnswer != null;
        return new QuestionResponse(
                base.id(),
                base.title(),
                base.statement(),
                base.statementHtml(),
                base.imageUrl(),
                base.subject(),
                base.topic(),
                base.subtopic(),
                base.difficulty(),
                base.year(),
                base.exam(),
                base.competency(),
                base.ability(),
                base.source(),
                base.sourceUrl(),
                base.sourceExam(),
                base.sourceYear(),
                base.sourceQuestionNumber(),
                base.sourceBookColor(),
                base.sourceDay(),
                base.sourcePage(),
                base.officialSourceUrl(),
                base.officialPdfUrl(),
                base.officialAnswerKeyUrl(),
                base.officialPage(),
                base.validatedAgainstOfficialSource(),
                base.validatedAt(),
                base.externalProvider(),
                base.externalProviderUrl(),
                base.externalQuestionId(),
                base.externalLicense(),
                base.statementHash(),
                base.importStatus(),
                answered ? base.explanation() : null,
                answered ? base.correctAlternative() : null,
                favorite,
                answered,
                answered ? latestAnswer.isCorrect() : null,
                base.assets(),
                base.alternatives(),
                base.createdAt(),
                base.updatedAt()
        );
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

    private List<QuestionImportStatus> reviewableStatuses() {
        return List.of(
                QuestionImportStatus.DRAFT,
                QuestionImportStatus.NEEDS_REVIEW,
                QuestionImportStatus.VALIDATED,
                QuestionImportStatus.INVALID
        );
    }

    private void updateErrorNotebook(User user, Question question, boolean correct) {
        ErrorNotebook notebook = errorNotebookRepository.findByUserIdAndQuestionId(user.getId(), question.getId())
                .orElseGet(() -> {
                    ErrorNotebook newNotebook = new ErrorNotebook();
                    newNotebook.setUser(user);
                    newNotebook.setQuestion(question);
                    return newNotebook;
                });

        if (correct) {
            notebook.setLastReviewedAt(LocalDate.now());
            notebook.setNextReviewAt(LocalDate.now().plusDays(7));
            notebook.setMasteryStatus(MasteryStatus.REVIEW);
            if (notebook.getId() != null) {
                errorNotebookRepository.save(notebook);
            }
            return;
        }

        int currentErrorCount = notebook.getId() == null
                ? 0
                : (notebook.getErrorCount() == null ? 0 : notebook.getErrorCount());
        notebook.setErrorCount(currentErrorCount + 1);
        notebook.setLastReviewedAt(LocalDate.now());
        notebook.setNextReviewAt(LocalDate.now().plusDays(1));
        notebook.setMasteryStatus(MasteryStatus.LEARNING);
        errorNotebookRepository.save(notebook);
    }
}
