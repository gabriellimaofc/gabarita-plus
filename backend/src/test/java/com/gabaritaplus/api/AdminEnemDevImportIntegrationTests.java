package com.gabaritaplus.api;

import com.gabaritaplus.api.dto.importer.enemdev.EnemDevAlternativeResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
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

    @MockBean
    private EnemDevApiClient enemDevApiClient;

    @Test
    @WithMockUser(roles = "ADMIN")
    void dryRunKeepsExternalQuestionsInNeedsReview() throws Exception {
        when(enemDevApiClient.listQuestions(eq(2023), eq(1), isNull(), isNull()))
                .thenReturn(List.of(new EnemDevQuestionResponse(
                        "Questao 1 - ENEM 2023",
                        1,
                        "linguagens",
                        "espanhol",
                        2023,
                        "Observe o grafico e responda a pergunta.",
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

    private String asJson(Object payload) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
    }
}
