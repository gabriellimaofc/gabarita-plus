package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewDetailResponse;
import com.gabaritaplus.api.dto.importer.review.AdminImportedQuestionReviewSummaryResponse;
import com.gabaritaplus.api.dto.importer.review.ValidateOfficialSourceRequest;
import com.gabaritaplus.api.dto.importer.review.UpdateImportedQuestionStatusRequest;
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
import com.gabaritaplus.api.entity.enums.AutoValidationStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.entity.enums.ReviewPriority;
import com.gabaritaplus.api.exception.ResourceNotFoundException;
import com.gabaritaplus.api.mapper.QuestionMapper;
import com.gabaritaplus.api.repository.ErrorNotebookRepository;
import com.gabaritaplus.api.repository.FavoriteQuestionRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.UserAnswerRepository;
import com.gabaritaplus.api.service.importer.QuestionImportSupport;
import com.gabaritaplus.api.service.importer.QuestionAutoValidationService;
import com.gabaritaplus.api.specification.QuestionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
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
    private final QuestionAutoValidationService questionAutoValidationService;

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

    public Page<AdminImportedQuestionReviewSummaryResponse> listReviewQuestions(
            List<QuestionImportStatus> statuses,
            String source,
            Integer year,
            String subject,
            AutoValidationStatus autoValidationStatus,
            Pageable pageable
    ) {
        Page<Question> page = questionRepository.findAll(
                QuestionSpecification.reviewQuestions(
                        resolveReviewStatuses(statuses),
                        normalizeSourceFilter(source),
                        year,
                        normalizeSubjectFilter(subject),
                        autoValidationStatus
                ),
                pageable
        );

        List<AdminImportedQuestionReviewSummaryResponse> content = page.getContent().stream()
                .map(this::toAdminReviewSummaryResponse)
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public AdminImportedQuestionReviewDetailResponse getReviewQuestion(Long id) {
        Question question = getQuestionEntity(id);
        initializeQuestionGraph(question);
        return toAdminReviewDetailResponse(question);
    }

    @Transactional
    public AdminImportedQuestionReviewDetailResponse updateReviewQuestionStatus(
            Long id,
            UpdateImportedQuestionStatusRequest request
    ) {
        Question question = getQuestionEntity(id);
        QuestionImportStatus targetStatus = request.importStatus();
        if (targetStatus == QuestionImportStatus.PUBLISHED) {
            throw new IllegalArgumentException("Use o endpoint de publicacao para publicar esta questao.");
        }
        question.setImportStatus(targetStatus);
        Question saved = questionRepository.save(question);
        initializeQuestionGraph(saved);
        return toAdminReviewDetailResponse(saved);
    }

    @Transactional
    public AdminImportedQuestionReviewDetailResponse validateOfficialSource(
            Long id,
            ValidateOfficialSourceRequest request
    ) {
        Question question = getQuestionEntity(id);
        question.setValidatedAgainstOfficialSource(true);
        question.setValidatedAt(OffsetDateTime.now());
        if (request.officialSourceUrl() != null && !request.officialSourceUrl().isBlank()) {
            question.setOfficialSourceUrl(request.officialSourceUrl().trim());
        }
        if (request.officialPdfUrl() != null && !request.officialPdfUrl().isBlank()) {
            question.setOfficialPdfUrl(request.officialPdfUrl().trim());
        }
        if (request.officialAnswerKeyUrl() != null && !request.officialAnswerKeyUrl().isBlank()) {
            question.setOfficialAnswerKeyUrl(request.officialAnswerKeyUrl().trim());
        }
        if (request.officialPage() != null) {
            question.setOfficialPage(request.officialPage());
        }
        if (question.getImportStatus() == QuestionImportStatus.NEEDS_REVIEW) {
            question.setImportStatus(QuestionImportStatus.VALIDATED);
        }
        Question saved = questionRepository.save(question);
        initializeQuestionGraph(saved);
        return toAdminReviewDetailResponse(saved);
    }

    @Transactional
    public AdminImportedQuestionReviewDetailResponse publishReviewQuestion(Long id) {
        Question question = getQuestionEntity(id);
        initializeQuestionGraph(question);
        questionAutoValidationService.applyAutoValidation(question);
        validatePublishable(question);
        question.setImportStatus(QuestionImportStatus.PUBLISHED);
        Question saved = questionRepository.save(question);
        initializeQuestionGraph(saved);
        return toAdminReviewDetailResponse(saved);
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
                QuestionImportStatus.AUTO_VALIDATED,
                QuestionImportStatus.INVALID
        );
    }

    private List<QuestionImportStatus> resolveReviewStatuses(List<QuestionImportStatus> requestedStatuses) {
        if (requestedStatuses == null || requestedStatuses.isEmpty()) {
            return reviewableStatuses();
        }
        return requestedStatuses;
    }

    private String normalizeSourceFilter(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return source.trim();
    }

    private String normalizeSubjectFilter(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        return subject.trim();
    }

    private void initializeQuestionGraph(Question question) {
        question.getAssets().size();
        question.getAlternatives().forEach(alternative -> alternative.getAssets().size());
        question.getAlternatives().size();
    }

    private AdminImportedQuestionReviewSummaryResponse toAdminReviewSummaryResponse(Question question) {
        initializeQuestionGraph(question);
        return new AdminImportedQuestionReviewSummaryResponse(
                question.getId(),
                question.getTitle(),
                question.getSource(),
                question.getSourceYear(),
                question.getSourceQuestionNumber(),
                question.getSourceBookColor(),
                question.getSourceDay(),
                question.getImportStatus(),
                question.getValidatedAgainstOfficialSource(),
                question.getExternalProvider(),
                questionRepository.findImportBatchIdByQuestionId(question.getId()),
                question.getSubject(),
                question.getDifficulty(),
                question.getCreatedAt(),
                question.getImportedAt(),
                question.getAlternatives().size(),
                question.getAssets().size(),
                question.getAutoValidationScore(),
                question.getAutoValidationStatus(),
                question.getAutoValidationWarnings(),
                question.getAutoValidationErrors(),
                question.getBrokenImageDetected(),
                question.getSuspiciousTextDetected(),
                question.getRequiresAssetReview()
        );
    }

    private AdminImportedQuestionReviewDetailResponse toAdminReviewDetailResponse(Question question) {
        return new AdminImportedQuestionReviewDetailResponse(
                question.getId(),
                question.getTitle(),
                question.getStatement(),
                question.getStatementHtml(),
                question.getImageUrl(),
                question.getSubject(),
                question.getTopic(),
                question.getSubtopic(),
                question.getDifficulty(),
                question.getYear(),
                question.getExam(),
                question.getCompetency(),
                question.getAbility(),
                question.getSource(),
                question.getSourceUrl(),
                question.getSourceExam(),
                question.getSourceYear(),
                question.getSourceQuestionNumber(),
                question.getSourceBookColor(),
                question.getSourceDay(),
                question.getSourcePage(),
                question.getOfficialSourceUrl(),
                question.getOfficialPdfUrl(),
                question.getOfficialAnswerKeyUrl(),
                question.getOfficialPage(),
                question.getValidatedAgainstOfficialSource(),
                question.getValidatedAt(),
                question.getExternalProvider(),
                question.getExternalProviderUrl(),
                question.getExternalQuestionId(),
                question.getExternalLicense(),
                question.getImportedAt(),
                questionRepository.findImportBatchIdByQuestionId(question.getId()),
                question.getStatementHash(),
                question.getImportStatus(),
                question.getExplanation(),
                question.getCorrectAlternative(),
                question.getAlternatives().size(),
                question.getAssets().size(),
                question.getAutoValidationScore(),
                question.getAutoValidationStatus(),
                question.getAutoValidationErrors(),
                question.getAutoValidationWarnings(),
                question.getAutoValidatedAt(),
                question.getBrokenImageDetected(),
                question.getSuspiciousTextDetected(),
                question.getRequiresAssetReview(),
                question.getAssets().stream().map(questionMapper::toAssetResponse).toList(),
                question.getAlternatives().stream().map(questionMapper::toAlternativeResponse).toList(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private void validatePublishable(Question question) {
        if (!Boolean.TRUE.equals(question.getValidatedAgainstOfficialSource())) {
            throw new IllegalArgumentException("A questao precisa ser validada contra a fonte oficial antes da publicacao.");
        }
        if (question.getCorrectAlternative() == null || question.getCorrectAlternative().isBlank()) {
            throw new IllegalArgumentException("A questao precisa ter gabarito antes da publicacao.");
        }
        if (question.getAlternatives().size() != 5) {
            throw new IllegalArgumentException("A questao precisa ter 5 alternativas antes da publicacao.");
        }
        if (hasBrokenVisualReference(question)) {
            throw new IllegalArgumentException("A questao possui imagem ou recurso visual quebrado e nao pode ser publicada.");
        }
        if (Boolean.TRUE.equals(question.getSuspiciousTextDetected())) {
            throw new IllegalArgumentException("A questao possui texto suspeito e precisa de revisao manual antes da publicacao.");
        }
        if (Boolean.TRUE.equals(question.getRequiresAssetReview())) {
            throw new IllegalArgumentException("A questao depende de asset visual pendente e nao pode ser publicada.");
        }
    }

    private boolean hasBrokenVisualReference(Question question) {
        if (containsBrokenImageUrl(question.getImageUrl())
                || containsBrokenImageUrl(question.getStatement())
                || containsBrokenImageUrl(question.getStatementHtml())) {
            return true;
        }
        if (question.getAssets().stream().anyMatch(asset ->
                containsBrokenImageUrl(asset.getUrl()) || containsBrokenImageUrl(asset.getStoragePath()))) {
            return true;
        }
        return question.getAlternatives().stream()
                .flatMap(alternative -> alternative.getAssets().stream())
                .anyMatch(asset -> containsBrokenImageUrl(asset.getUrl()) || containsBrokenImageUrl(asset.getStoragePath()));
    }

    private boolean containsBrokenImageUrl(String value) {
        return value != null && value.toLowerCase().contains("broken-image");
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
