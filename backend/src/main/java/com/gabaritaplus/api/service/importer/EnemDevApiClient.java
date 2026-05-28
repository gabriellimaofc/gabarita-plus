package com.gabaritaplus.api.service.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevExamResponse;
import com.gabaritaplus.api.dto.importer.enemdev.EnemDevQuestionsPageResponse;
import com.gabaritaplus.api.exception.ExternalRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EnemDevApiClient {

    private static final int MAX_RETRIES = 5;
    private static final int MIN_WAIT_MS = 1_000;
    private static final int SAFETY_MARGIN_MS = 500;
    private static final int MAX_PAGE_LIMIT = 20;
    private static final Pattern RETRY_AFTER_MS_PATTERN = Pattern.compile("Try again in\\s+(\\d+)ms", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EnemDevApiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.import.enem-dev.base-url:https://api.enem.dev/v1}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
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

    public List<EnemDevQuestionResponse> listQuestions(
            Integer year,
            Integer requestedLimit,
            Integer requestedOffset,
            String language
    ) {
        int safeLimit = sanitizeLimit(requestedLimit);
        int offset = requestedOffset == null ? 0 : Math.max(requestedOffset, 0);
        EnemDevQuestionsPageResponse page = fetchQuestionsPage(year, safeLimit, offset, language, 1);
        if (page == null || page.questions() == null) {
            return List.of();
        }
        return page.questions().stream().limit(safeLimit).toList();
    }

    public List<EnemDevQuestionResponse> listAllQuestions(
            Integer year,
            Integer requestedLimit,
            Integer requestedOffset,
            String language
    ) {
        int pageLimit = MAX_PAGE_LIMIT;
        int offset = requestedOffset == null ? 0 : Math.max(requestedOffset, 0);
        Integer remaining = requestedLimit == null ? null : Math.max(requestedLimit, 0);
        List<EnemDevQuestionResponse> questions = new ArrayList<>();
        boolean hasMore = true;

        while (hasMore && (remaining == null || remaining > 0)) {
            int currentLimit = remaining == null ? pageLimit : Math.min(pageLimit, remaining);
            EnemDevQuestionsPageResponse page = fetchQuestionsPage(year, currentLimit, offset, language, 1);

            if (page == null || page.questions() == null || page.questions().isEmpty()) {
                break;
            }

            List<EnemDevQuestionResponse> pageQuestions = page.questions().stream().limit(currentLimit).toList();
            questions.addAll(pageQuestions);
            hasMore = Boolean.TRUE.equals(page.metadata() == null ? null : page.metadata().hasMore());
            offset += pageQuestions.size();
            if (remaining != null) {
                remaining -= pageQuestions.size();
            }
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
            if (exception.getStatusCode().value() == 429) {
                long waitMs = calculateWaitMs(exception, attempt);
                if (attempt < MAX_RETRIES) {
                    log.warn(
                            "enem.dev rate limit em year={}, offset={}, limit={}, attempt={}/{}. Aguardando {}ms antes do retry.",
                            year,
                            offset,
                            limit,
                            attempt,
                            MAX_RETRIES,
                            waitMs
                    );
                    sleep(waitMs);
                    return fetchQuestionsPage(year, limit, offset, language, attempt + 1);
                }
                log.warn(
                        "enem.dev continuou limitando requisicoes apos {} tentativas. year={}, offset={}, limit={}",
                        attempt,
                        year,
                        offset,
                        limit
                );
                throw new ExternalRateLimitException(
                        "A fonte externa enem.dev limitou as requisicoes. Tente novamente em alguns segundos."
                );
            }
            throw exception;
        }
    }

    private int sanitizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return 1;
        }
        return Math.min(requestedLimit, MAX_PAGE_LIMIT);
    }

    private long calculateWaitMs(RestClientResponseException exception, int attempt) {
        long informedWaitMs = extractRetryAfterMs(exception.getResponseBodyAsString());
        long baseBackoffMs = (long) MIN_WAIT_MS * (1L << Math.max(0, attempt - 1));
        long jitterMs = ThreadLocalRandom.current().nextLong(150L, 451L);
        long candidateWaitMs = Math.max(baseBackoffMs, informedWaitMs > 0 ? informedWaitMs + SAFETY_MARGIN_MS : 0L);
        return Math.max(MIN_WAIT_MS, candidateWaitMs + jitterMs);
    }

    private long extractRetryAfterMs(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return 0L;
        }

        Matcher matcher = RETRY_AFTER_MS_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messageNode = root.path("error").path("message");
            if (!messageNode.isTextual()) {
                return 0L;
            }
            Matcher bodyMatcher = RETRY_AFTER_MS_PATTERN.matcher(messageNode.asText());
            return bodyMatcher.find() ? Long.parseLong(bodyMatcher.group(1)) : 0L;
        } catch (Exception exception) {
            log.debug("Nao foi possivel extrair retry-after do body retornado pelo enem.dev.", exception);
            return 0L;
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
