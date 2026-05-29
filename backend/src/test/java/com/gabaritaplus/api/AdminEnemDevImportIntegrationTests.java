package com.gabaritaplus.api;

import com.gabaritaplus.api.dto.importer.enemdev.EnemDevAlternativeResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse;
import com.gabaritaplus.api.entity.Alternative;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.entity.enums.RoleName;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.RoleRepository;
import com.gabaritaplus.api.repository.UserRepository;
import com.gabaritaplus.api.security.JwtService;
import com.gabaritaplus.api.service.importer.EnemDevApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
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
class AdminEnemDevImportIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EnemDevApiClient enemDevApiClient;

    @Test
    @WithMockUser(roles = "ADMIN")
    void dryRunKeepsExternalQuestionsInNeedsReview() throws Exception {
        when(enemDevApiClient.listQuestions(eq(2023), eq(1), isNull(), isNull()))
                .thenReturn(List.of(new EnemDevQuestionResponse(
                        "QuestÃ£o 1 - ENEM 2023",
                        1,
                        "linguagens",
                        "espanhol",
                        2023,
                        "Observe o grÃ¡fico e responda Ã  pergunta.",
                        List.of(),
                        "A",
                        "Selecione a alternativa correta.",
                        List.of(
                                new EnemDevAlternativeResponse("A", "Alternativa A", null, true),
                                new EnemDevAlternativeResponse("B", "Alternativa B", null, false),
                                new EnemDevAlternativeResponse("C", "Alternativa C", null, false),
                                new EnemDevAlternativeResponse("D", "Alternativa D", null, false),
                                new EnemDevAlternativeResponse("E", "Alternativa E", null, false)
                        )
                )));

        mockMvc.perform(post("/admin/import/enem-dev/dry-run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("year", 2023))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.totalProcessed").value(1))
                .andExpect(jsonPath("$.data.imported").value(0))
                .andExpect(jsonPath("$.data.needsReview").value(1))
                .andExpect(jsonPath("$.data.invalid").value(0))
                .andExpect(jsonPath("$.data.batchId").isEmpty())
                .andExpect(jsonPath("$.data.itemErrors[0].title").value("Questão 1 - ENEM 2023"))
                .andExpect(jsonPath("$.data.itemErrors[0].errors[0]")
                        .value("Questao marcada como NEEDS_REVIEW por completude ou texto suspeito."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void dryRunRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/admin/import/enem-dev/dry-run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("year", 2023))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reviewEndpointsListAndDetailNeedReviewQuestions() throws Exception {
        Question question = buildReviewQuestion();
        Question saved = questionRepository.save(question);
        Question published = questionRepository.save(buildPublishedQuestion());

        mockMvc.perform(get("/admin/import/questions/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(saved.getId().intValue())))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(published.getId().intValue()))))
                .andExpect(jsonPath("$.data[0].importStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data[0].source").value("ENEM_DEV"))
                .andExpect(jsonPath("$.data[0].validatedAgainstOfficialSource").value(false))
                .andExpect(jsonPath("$.data[0].importBatchId").isEmpty())
                .andExpect(jsonPath("$.data[0].alternativesCount").value(0))
                .andExpect(jsonPath("$.data[0].assetsCount").value(0));

        mockMvc.perform(get("/admin/import/questions/review")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(published.getId().intValue())));

        mockMvc.perform(get("/admin/import/questions/review")
                        .param("status", "NEEDS_REVIEW")
                        .param("source", "ENEM_DEV")
                        .param("year", "2023")
                        .param("subject", "Linguagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(saved.getId().intValue())));

        mockMvc.perform(get("/admin/import/questions/review/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.importStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.source").value("ENEM_DEV"))
                .andExpect(jsonPath("$.data.externalProvider").value("enem.dev"))
                .andExpect(jsonPath("$.data.validatedAgainstOfficialSource").value(false))
                .andExpect(jsonPath("$.data.importBatchId").isEmpty())
                .andExpect(jsonPath("$.data.alternativesCount").value(0))
                .andExpect(jsonPath("$.data.assetsCount").value(0))
                .andExpect(jsonPath("$.data.alternatives").isArray())
                .andExpect(jsonPath("$.data.assets").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void reviewEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/import/questions/review"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/import/questions/review/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reviewStatusActionsKeepPublicationControlled() throws Exception {
        Question reviewQuestion = buildReviewQuestionWithAlternatives();
        reviewQuestion.setStatement("Leia o texto e responda a pergunta.");
        reviewQuestion.setSourceDay(1);
        reviewQuestion.setSourceBookColor("AZUL");
        Question saved = questionRepository.save(reviewQuestion);

        mockMvc.perform(patch("/admin/import/questions/review/{id}/status", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("importStatus", "INVALID"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importStatus").value("INVALID"))
                .andExpect(jsonPath("$.data.validatedAgainstOfficialSource").value(false));

        mockMvc.perform(patch("/admin/import/questions/review/{id}/validate-official-source", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "officialSourceUrl", "https://www.gov.br/inep/prova.pdf",
                                "officialPdfUrl", "https://www.gov.br/inep/prova.pdf",
                                "officialAnswerKeyUrl", "https://www.gov.br/inep/gabarito.pdf",
                                "officialPage", 12
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validatedAgainstOfficialSource").value(true))
                .andExpect(jsonPath("$.data.importStatus").value("INVALID"));

        mockMvc.perform(post("/admin/import/questions/review/{id}/publish", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.validatedAgainstOfficialSource").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void publishBlocksBrokenImagesAndMissingOfficialValidation() throws Exception {
        Question saved = questionRepository.save(buildReviewQuestionWithAlternatives());
        saved.setStatement("![](https://enem.dev/broken-image.svg)");
        saved.setValidatedAgainstOfficialSource(false);
        questionRepository.save(saved);

        mockMvc.perform(post("/admin/import/questions/review/{id}/publish", saved.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void autoValidationKeepsBrokenImageInReviewAndBlocksPublish() throws Exception {
        Question saved = buildReviewQuestionWithAlternatives();
        saved.setStatement("Texto com ![](https://enem.dev/broken-image.svg)");
        saved.setValidatedAgainstOfficialSource(true);
        saved = questionRepository.save(saved);

        mockMvc.perform(post("/admin/import/questions/{id}/auto-validate", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.autoValidationStatus").value("NEEDS_HUMAN_REVIEW"))
                .andExpect(jsonPath("$.data.brokenImageDetected").value(true))
                .andExpect(jsonPath("$.data.autoValidationWarnings").value(org.hamcrest.Matchers.containsString("ASSET_MISSING_OR_BROKEN")));

        mockMvc.perform(post("/admin/import/questions/review/{id}/publish", saved.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void autoValidationScoresSafeQuestionButAutoPublishFlagKeepsItUnpublished() throws Exception {
        Question saved = buildReviewQuestionWithAlternatives();
        saved.setValidatedAgainstOfficialSource(true);
        saved.setSourceDay(1);
        saved.setSourceBookColor("AZUL");
        saved.setStatement("Leia o texto e responda a pergunta.");
        saved = questionRepository.save(saved);

        mockMvc.perform(post("/admin/import/questions/{id}/auto-validate", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoValidationScore").value(100))
                .andExpect(jsonPath("$.data.autoValidationStatus").value("SAFE_TO_AUTO_VALIDATE"))
                .andExpect(jsonPath("$.data.importStatus").value("AUTO_VALIDATED"));

        mockMvc.perform(post("/admin/import/questions/auto-publish-safe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void autoValidationDetectsSuspiciousTextAndMissingOfficialValidation() throws Exception {
        Question saved = buildReviewQuestionWithAlternatives();
        saved.setStatement("ТЕХТО II com conteudo suspeito.");
        saved.setValidatedAgainstOfficialSource(false);
        saved = questionRepository.save(saved);

        mockMvc.perform(post("/admin/import/questions/{id}/auto-validate", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.autoValidationStatus").value("NEEDS_HUMAN_REVIEW"))
                .andExpect(jsonPath("$.data.suspiciousTextDetected").value(true))
                .andExpect(jsonPath("$.data.validatedAgainstOfficialSource").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void officialSourceValidationRequiresStructuredAnswerKey() throws Exception {
        Question saved = buildReviewQuestionWithAlternatives();
        saved.setStatement("Leia o texto e responda a pergunta.");
        saved.setSourceDay(1);
        saved.setSourceBookColor("AZUL");
        saved = questionRepository.save(saved);

        mockMvc.perform(post("/admin/import/official-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "exam", "ENEM",
                                "year", 2023,
                                "day", 1,
                                "bookColor", "AZUL",
                                "pdfUrl", "https://www.gov.br/inep/prova.pdf",
                                "answerKeyUrl", "https://www.gov.br/inep/gabarito.pdf",
                                "sourceUrl", "https://www.gov.br/inep/provas-e-gabaritos",
                                "answerKeyMapJson", "{\"1\":\"A\"}"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/import/questions/{id}/validate-against-official-source", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(1))
                .andExpect(jsonPath("$.data.validated").value(1))
                .andExpect(jsonPath("$.data.items[0].validatedAgainstOfficialSource").value(true))
                .andExpect(jsonPath("$.data.items[0].importStatus").value("AUTO_VALIDATED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void officialAssetRecoveryKeepsUntrustedVisualQuestionInReview() throws Exception {
        Question saved = buildReviewQuestionWithAlternatives();
        saved.setStatement("Texto com ![](https://enem.dev/broken-image.svg)");
        saved.setSourceDay(1);
        saved.setSourceBookColor("AZUL");
        saved = questionRepository.save(saved);

        mockMvc.perform(post("/admin/import/official-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "exam", "ENEM",
                                "year", 2023,
                                "day", 1,
                                "bookColor", "AZUL",
                                "pdfUrl", "https://www.gov.br/inep/prova.pdf",
                                "answerKeyUrl", "https://www.gov.br/inep/gabarito.pdf",
                                "sourceUrl", "https://www.gov.br/inep/provas-e-gabaritos"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/import/questions/{id}/recover-assets", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(1))
                .andExpect(jsonPath("$.data.assetRecovered").value(0))
                .andExpect(jsonPath("$.data.assetRecoveryFailed").value(1))
                .andExpect(jsonPath("$.data.items[0].warnings", hasItem("ASSET_RECOVERY_FAILED")));

        Question reloaded = questionRepository.findById(saved.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(QuestionImportStatus.NEEDS_REVIEW, reloaded.getImportStatus());
    }

    @Test
    void needsReviewQuestionStaysHiddenFromStudentEndpoints() throws Exception {
        Question saved = questionRepository.save(buildReviewQuestion());
        User student = createUser();

        mockMvc.perform(get("/questions/{id}", saved.getId())
                        .header("Authorization", authorizationHeader(student)))
                .andExpect(status().isNotFound());
    }

    private String asJson(Object payload) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
    }

    private Question buildReviewQuestion() {
        Question question = new Question();
        question.setTitle("Questao em revisao ENEM_DEV");
        question.setStatement("Observe o grafico e responda.");
        question.setSubject("Linguagens");
        question.setTopic("A classificar");
        question.setDifficulty(DifficultyLevel.MEDIUM);
        question.setYear(2023);
        question.setExam("ENEM");
        question.setCorrectAlternative("A");
        question.setSource("ENEM_DEV");
        question.setSourceUrl("https://api.enem.dev/v1/exams/2023/questions/1");
        question.setSourceExam("ENEM");
        question.setSourceYear(2023);
        question.setSourceQuestionNumber(1);
        question.setSourceBookColor("UNKNOWN");
        question.setImportStatus(QuestionImportStatus.NEEDS_REVIEW);
        question.setValidatedAgainstOfficialSource(false);
        question.setExternalProvider("enem.dev");
        question.setExternalProviderUrl("https://enem.dev");
        question.setExternalQuestionId("2023:1:linguagens:default");
        question.setStatementHash("review-test-hash");
        return question;
    }

    private Question buildPublishedQuestion() {
        Question question = buildReviewQuestion();
        question.setTitle("Questao publicada");
        question.setImportStatus(QuestionImportStatus.PUBLISHED);
        question.setStatementHash("published-review-test-hash");
        return question;
    }

    private Question buildReviewQuestionWithAlternatives() {
        Question question = buildReviewQuestion();
        question.getAlternatives().clear();
        for (String letter : List.of("A", "B", "C", "D", "E")) {
            Alternative alternative = new Alternative();
            alternative.setLetter(letter);
            alternative.setText("Alternativa " + letter);
            alternative.setCorrect("A".equals(letter));
            alternative.setQuestion(question);
            question.getAlternatives().add(alternative);
        }
        return question;
    }

    private User createUser() {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ROLE_USER);
                    role.setDescription("Aluno");
                    return roleRepository.save(role);
                });

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setFullName("Aluno " + suffix);
        user.setEmail("aluno." + suffix + "@example.com");
        user.setUsername("aluno" + suffix);
        user.setPassword(passwordEncoder.encode("User@123"));
        user.getRoles().add(userRole);
        return userRepository.save(user);
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
}
