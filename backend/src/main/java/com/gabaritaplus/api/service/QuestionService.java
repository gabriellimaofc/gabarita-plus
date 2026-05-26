package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.question.ErrorNotebookResponse;
import com.gabaritaplus.api.dto.question.QuestionFilterRequest;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.dto.question.QuestionResponse;
import com.gabaritaplus.api.dto.question.UserAnswerRequest;
import com.gabaritaplus.api.dto.question.UserAnswerResponse;
import com.gabaritaplus.api.entity.ErrorNotebook;
import com.gabaritaplus.api.entity.FavoriteQuestion;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.UserAnswer;
import com.gabaritaplus.api.entity.enums.MasteryStatus;
import com.gabaritaplus.api.exception.ResourceNotFoundException;
import com.gabaritaplus.api.mapper.QuestionMapper;
import com.gabaritaplus.api.repository.ErrorNotebookRepository;
import com.gabaritaplus.api.repository.FavoriteQuestionRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.UserAnswerRepository;
import com.gabaritaplus.api.specification.QuestionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        Question question = questionMapper.toEntity(request);
        Question savedQuestion = questionRepository.save(question);
        return enrichResponse(savedQuestion, authenticatedUserService.getCurrentUser());
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Question existing = getQuestionEntity(id);
        existing.setTitle(request.title());
        existing.setStatement(request.statement());
        existing.setImageUrl(request.imageUrl());
        existing.setSubject(request.subject());
        existing.setTopic(request.topic());
        existing.setSubtopic(request.subtopic());
        existing.setDifficulty(request.difficulty());
        existing.setYear(request.year());
        existing.setExam(request.exam());
        existing.setCompetency(request.competency());
        existing.setAbility(request.ability());
        existing.setExplanation(request.explanation());
        existing.setCorrectAlternative(request.correctAlternative());
        existing.getAlternatives().clear();
        request.alternatives().forEach(alternativeRequest -> {
            var alternative = questionMapper.toAlternative(alternativeRequest);
            alternative.setQuestion(existing);
            existing.getAlternatives().add(alternative);
        });
        return enrichResponse(questionRepository.save(existing), authenticatedUserService.getCurrentUser());
    }

    public QuestionResponse getById(Long id) {
        return enrichResponse(getQuestionEntity(id), authenticatedUserService.getCurrentUser());
    }

    public Page<QuestionResponse> search(QuestionFilterRequest filter, Pageable pageable) {
        User user = authenticatedUserService.getCurrentUser();
        List<Long> answeredIds = userAnswerRepository.findAnsweredQuestionIdsByUserId(user.getId());
        List<Long> incorrectIds = userAnswerRepository.findIncorrectQuestionIdsByUserId(user.getId());
        List<Long> favoriteIds = favoriteQuestionRepository.findFavoriteQuestionIdsByUserId(user.getId());

        return questionRepository.findAll(QuestionSpecification.byFilters(filter, answeredIds, incorrectIds, favoriteIds), pageable)
                .map(question -> enrichResponse(question, user));
    }

    @Transactional
    public void delete(Long id) {
        Question question = getQuestionEntity(id);
        questionRepository.delete(question);
    }

    @Transactional
    public UserAnswerResponse answerQuestion(UserAnswerRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        Question question = getQuestionEntity(request.questionId());
        int attemptNumber = userAnswerRepository.findTopByUserIdAndQuestionIdOrderByAttemptNumberDesc(user.getId(), question.getId())
                .map(answer -> answer.getAttemptNumber() + 1)
                .orElse(1);
        boolean correct = question.getCorrectAlternative().equalsIgnoreCase(request.chosenAlternative());

        UserAnswer answer = new UserAnswer();
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setChosenAlternative(request.chosenAlternative().toUpperCase());
        answer.setTimeSpentSeconds(request.timeSpentSeconds());
        answer.setCorrect(correct);
        answer.setAttemptNumber(attemptNumber);

        UserAnswer savedAnswer = userAnswerRepository.save(answer);
        updateErrorNotebook(user, question, correct);
        log.info("Resposta registrada. userId={}, questionId={}, correct={}", user.getId(), question.getId(), correct);
        return questionMapper.toAnswerResponse(savedAnswer);
    }

    @Transactional
    public QuestionResponse toggleFavorite(Long questionId) {
        User user = authenticatedUserService.getCurrentUser();
        Question question = getQuestionEntity(questionId);
        favoriteQuestionRepository.findByUserIdAndQuestionId(user.getId(), questionId)
                .ifPresentOrElse(favoriteQuestionRepository::delete, () -> {
                    FavoriteQuestion favoriteQuestion = new FavoriteQuestion();
                    favoriteQuestion.setUser(user);
                    favoriteQuestion.setQuestion(question);
                    favoriteQuestionRepository.save(favoriteQuestion);
                });
        return enrichResponse(question, user);
    }

    public List<ErrorNotebookResponse> getErrorNotebook() {
        User user = authenticatedUserService.getCurrentUser();
        return errorNotebookRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(questionMapper::toErrorNotebookResponse)
                .toList();
    }

    private Question getQuestionEntity(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada."));
    }

    private QuestionResponse enrichResponse(Question question, User user) {
        QuestionResponse base = questionMapper.toResponse(question);
        boolean favorite = favoriteQuestionRepository.findByUserIdAndQuestionId(user.getId(), question.getId()).isPresent();
        var latestAnswer = userAnswerRepository.findTopByUserIdAndQuestionIdOrderByAttemptNumberDesc(user.getId(), question.getId());
        return new QuestionResponse(
                base.id(),
                base.title(),
                base.statement(),
                base.imageUrl(),
                base.subject(),
                base.topic(),
                base.subtopic(),
                base.difficulty(),
                base.year(),
                base.exam(),
                base.competency(),
                base.ability(),
                base.explanation(),
                base.correctAlternative(),
                favorite,
                latestAnswer.isPresent(),
                latestAnswer.map(UserAnswer::isCorrect).orElse(null),
                base.alternatives(),
                base.createdAt(),
                base.updatedAt()
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

        notebook.setErrorCount((notebook.getErrorCount() == null ? 0 : notebook.getErrorCount()) + 1);
        notebook.setLastReviewedAt(LocalDate.now());
        notebook.setNextReviewAt(LocalDate.now().plusDays(1));
        notebook.setMasteryStatus(MasteryStatus.LEARNING);
        errorNotebookRepository.save(notebook);
    }
}
