package com.gabaritaplus.api;

import com.gabaritaplus.api.dto.importer.enemdev.EnemDevAlternativeResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.service.importer.EnemDevApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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

        mockMvc.perform(get("/admin/import/questions/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(saved.getId().intValue())))
                .andExpect(jsonPath("$.data[0].importStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data[0].source").value("ENEM_DEV"))
                .andExpect(jsonPath("$.data[0].validatedAgainstOfficialSource").value(false));

        mockMvc.perform(get("/admin/import/questions/review/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.importStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.source").value("ENEM_DEV"))
                .andExpect(jsonPath("$.data.externalProvider").value("enem.dev"))
                .andExpect(jsonPath("$.data.validatedAgainstOfficialSource").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void reviewEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/import/questions/review"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/import/questions/review/1"))
                .andExpect(status().isForbidden());
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
}
