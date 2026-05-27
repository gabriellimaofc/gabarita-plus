package com.gabaritaplus.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabaritaplus.api.entity.Alternative;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.RoleName;
import com.gabaritaplus.api.repository.ErrorNotebookRepository;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.RoleRepository;
import com.gabaritaplus.api.repository.UserAnswerRepository;
import com.gabaritaplus.api.repository.UserRepository;
import com.gabaritaplus.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.seed.enabled=false",
        "logging.level.org.springframework=warn",
        "logging.level.com.gabaritaplus.api=warn"
})
class StudyFlowsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private ErrorNotebookRepository errorNotebookRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicAuthRoutesIgnoreInvalidBearerToken() throws Exception {
        User user = createUser("User@123");

        mockMvc.perform(post("/auth/login")
                        .header("Authorization", "Bearer token-antigo-ou-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "usernameOrEmail", user.getEmail(),
                                "password", "User@123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer token-antigo-ou-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "fullName", "Novo Aluno",
                                "email", "novo." + UUID.randomUUID() + "@example.com",
                                "username", "novo" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                                "password", "User@123",
                                "targetCourse", "Medicina"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void questionAnswerFlowKeepsAnswerKeyHiddenUntilAnsweredAndUpdatesErrorNotebook() throws Exception {
        User user = createUser();
        Question question = createQuestion(
                "Matematica",
                "Funcoes",
                "A",
                "A alternativa correta relaciona crescimento e taxa de variacao."
        );

        mockMvc.perform(get("/questions/{id}", question.getId())
                        .header("Authorization", authorizationHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctAlternative").doesNotExist())
                .andExpect(jsonPath("$.data.explanation").doesNotExist())
                .andExpect(jsonPath("$.data.alternatives[0].correct").doesNotExist());

        mockMvc.perform(post("/questions/answers")
                        .header("Authorization", authorizationHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "questionId", question.getId(),
                                "chosenAlternative", "B",
                                "timeSpentSeconds", 32
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.correctAlternative").value("A"))
                .andExpect(jsonPath("$.data.explanation").value(
                        "A alternativa correta relaciona crescimento e taxa de variacao."
                ));

        mockMvc.perform(get("/questions/error-notebook")
                        .header("Authorization", authorizationHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionId").value(question.getId()))
                .andExpect(jsonPath("$.data[0].errorCount").value(1))
                .andExpect(jsonPath("$.data[0].priority").value("HIGH"));

        mockMvc.perform(patch("/questions/error-notebook/{questionId}", question.getId())
                        .header("Authorization", authorizationHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("masteryStatus", "MASTERED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.masteryStatus").value("MASTERED"))
                .andExpect(jsonPath("$.data.priority").value("LOW"));

        assertThat(errorNotebookRepository.findByUserIdAndQuestionId(user.getId(), question.getId()))
                .isPresent();
    }

    @Test
    void mockExamFlowCreatesAnswersFinishesAndPersistsDetailedResult() throws Exception {
        User user = createUser();
        Question questionOne = createQuestion(
                "Linguagens",
                "Interpretacao",
                "A",
                "A resposta correta depende da leitura do tom do texto."
        );
        Question questionTwo = createQuestion(
                "Linguagens",
                "Literatura",
                "C",
                "A escola literaria e identificada pelos traços simbolistas."
        );

        String createResponse = mockMvc.perform(post("/mock-exams")
                        .header("Authorization", authorizationHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "title", "Simulado integracao",
                                "durationMinutes", 90,
                                "questionIds", List.of(questionOne.getId(), questionTwo.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdExam = objectMapper.readTree(createResponse);
        long mockExamId = createdExam.path("data").path("id").asLong();

        mockMvc.perform(post("/mock-exams/{id}/answers", mockExamId)
                        .header("Authorization", authorizationHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "questionId", questionOne.getId(),
                                "chosenAlternative", "A",
                                "timeSpentSeconds", 41
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answeredCount").value(1));

        mockMvc.perform(post("/mock-exams/{id}/answers", mockExamId)
                        .header("Authorization", authorizationHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "questionId", questionTwo.getId(),
                                "chosenAlternative", "B",
                                "timeSpentSeconds", 52
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answeredCount").value(2));

        mockMvc.perform(get("/mock-exams/{id}/questions", mockExamId)
                        .header("Authorization", authorizationHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].chosenAlternative").value("A"))
                .andExpect(jsonPath("$.data[0].correctAlternative").doesNotExist())
                .andExpect(jsonPath("$.data[0].explanation").doesNotExist());

        mockMvc.perform(post("/mock-exams/{id}/finish", mockExamId)
                        .header("Authorization", authorizationHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("timeSpentSeconds", 3000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finished").value(true))
                .andExpect(jsonPath("$.data.finalScore").value(50.0))
                .andExpect(jsonPath("$.data.correctAnswers").value(1))
                .andExpect(jsonPath("$.data.incorrectAnswers").value(1))
                .andExpect(jsonPath("$.data.performanceBySubject[0].subject").value("Linguagens"));

        mockMvc.perform(get("/mock-exams/{id}/result", mockExamId)
                        .header("Authorization", authorizationHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].correctAlternative").value("A"))
                .andExpect(jsonPath("$.data.questions[1].correctAlternative").value("C"))
                .andExpect(jsonPath("$.data.questions[1].correct").value(false));

        assertThat(userAnswerRepository.countByUserId(user.getId())).isEqualTo(2);
        assertThat(errorNotebookRepository.findByUserIdAndQuestionId(user.getId(), questionTwo.getId()))
                .isPresent();
    }

    private User createUser() {
        return createUser("$2a$10$abcdefghijklmnopqrstuv");
    }

    private User createUser(String rawPassword) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ROLE_USER);
                    role.setDescription("Aluno");
                    return roleRepository.save(role);
                });

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setFullName("Teste " + suffix);
        user.setEmail("teste." + suffix + "@example.com");
        user.setUsername("teste" + suffix);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    private Question createQuestion(
            String subject,
            String topic,
            String correctAlternative,
            String explanation
    ) {
        Question question = new Question();
        question.setTitle("Questao " + UUID.randomUUID());
        question.setStatement("Enunciado de teste para " + topic + ".");
        question.setSubject(subject);
        question.setTopic(topic);
        question.setDifficulty(DifficultyLevel.MEDIUM);
        question.setYear(2025);
        question.setExam("ENEM");
        question.setExplanation(explanation);
        question.setCorrectAlternative(correctAlternative);

        question.getAlternatives().add(createAlternative(question, "A", "Alternativa A", "A".equals(correctAlternative)));
        question.getAlternatives().add(createAlternative(question, "B", "Alternativa B", "B".equals(correctAlternative)));
        question.getAlternatives().add(createAlternative(question, "C", "Alternativa C", "C".equals(correctAlternative)));
        question.getAlternatives().add(createAlternative(question, "D", "Alternativa D", "D".equals(correctAlternative)));
        question.getAlternatives().add(createAlternative(question, "E", "Alternativa E", "E".equals(correctAlternative)));

        return questionRepository.save(question);
    }

    private Alternative createAlternative(
            Question question,
            String letter,
            String text,
            boolean correct
    ) {
        Alternative alternative = new Alternative();
        alternative.setQuestion(question);
        alternative.setLetter(letter);
        alternative.setText(text);
        alternative.setCorrect(correct);
        return alternative;
    }

    private String authorizationHeader(User user) {
        return "Bearer " + generateAccessToken(user);
    }

    private String generateAccessToken(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("ignored")
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .toList())
                .build();
        return jwtService.generateAccessToken(userDetails);
    }

    private String asJson(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }
}
