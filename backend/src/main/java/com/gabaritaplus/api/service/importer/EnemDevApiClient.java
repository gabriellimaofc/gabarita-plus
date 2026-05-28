package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.dto.importer.enemdev.EnemDevExamResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionsPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class EnemDevApiClient {

    private final RestClient restClient;

    public EnemDevApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.import.enem-dev.base-url:https://api.enem.dev/v1}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<EnemDevExamResponse> listExams() {
        EnemDevExamResponse[] response = restClient.get()
                .uri("/exams")
                .retrieve()
                .body(EnemDevExamResponse[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    public EnemDevExamResponse getExam(Integer year) {
        return restClient.get()
                .uri("/exams/{year}", year)
                .retrieve()
                .body(EnemDevExamResponse.class);
    }

    public List<com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse> listAllQuestions(
            Integer year,
            Integer requestedLimit,
            Integer requestedOffset,
            String language
    ) {
        int limit = requestedLimit == null ? 50 : requestedLimit;
        int offset = requestedOffset == null ? 0 : requestedOffset;
        List<com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse> questions = new ArrayList<>();
        boolean hasMore = true;

        while (hasMore) {
            EnemDevQuestionsPageResponse page = fetchQuestionsPage(year, limit, offset, language, 1);

            if (page == null || page.questions() == null || page.questions().isEmpty()) {
                break;
            }

            questions.addAll(page.questions());
            hasMore = Boolean.TRUE.equals(page.metadata() == null ? null : page.metadata().hasMore());
            offset += limit;
        }

        log.info("enem.dev: {} questoes carregadas para o ano {}", questions.size(), year);
        return questions;
    }

    private EnemDevQuestionsPageResponse fetchQuestionsPage(
            Integer year,
            int limit,
            int offset,
            String language,
            int attempt
    ) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/exams/{year}/questions")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (language != null && !language.isBlank()) {
                            builder.queryParam("language", language);
                        }
                        return builder.build(year);
                    })
                    .retrieve()
                    .body(EnemDevQuestionsPageResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429 && attempt <= 5) {
                long delayMs = 300L * attempt;
                log.warn("enem.dev rate limit em year={}, offset={}, attempt={}. Aguardando {}ms.", year, offset, attempt, delayMs);
                sleep(delayMs);
                return fetchQuestionsPage(year, limit, offset, language, attempt + 1);
            }
            throw exception;
        }
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompida durante retry do enem.dev.", exception);
        }
    }
}
