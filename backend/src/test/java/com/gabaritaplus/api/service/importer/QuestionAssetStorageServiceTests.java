package com.gabaritaplus.api.service.importer;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QuestionAssetStorageServiceTests {

    private static final String SUPABASE_URL = "https://project.supabase.co";
    private static final String BUCKET = "question-assets";

    @Test
    void newSecretKeyUsesApiKeyHeaderWithoutBearerAuthorization() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QuestionAssetStorageService service = service(builder, "sb_secret_test", "");

        expectStorageRequests(server, "sb_secret_test", null);

        service.storePng("enem/question.png", new byte[]{1, 2, 3});

        server.verify();
    }

    @Test
    void legacyServiceRoleKeyRetainsBearerAuthorization() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QuestionAssetStorageService service = service(builder, "", "legacy-jwt-test");

        expectStorageRequests(server, "legacy-jwt-test", "Bearer legacy-jwt-test");

        service.storePng("enem/question.png", new byte[]{1, 2, 3});

        server.verify();
    }

    @Test
    void newSecretKeyHasPriorityOverLegacyFallback() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QuestionAssetStorageService service = service(builder, "sb_secret_primary", "legacy-jwt-fallback");

        expectStorageRequests(server, "sb_secret_primary", null);

        service.storePng("enem/question.png", new byte[]{1, 2, 3});

        server.verify();
    }

    @Test
    void remoteErrorCannotExposeSecretInExceptionMessages() {
        String secret = "sb_secret_must_not_leak";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QuestionAssetStorageService service = service(builder, secret, "");
        server.expect(requestTo(SUPABASE_URL + "/storage/v1/bucket/" + BUCKET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(secret));

        QuestionAssetStorageException exception = assertThrows(
                QuestionAssetStorageException.class,
                () -> service.storePng("enem/question.png", new byte[]{1, 2, 3})
        );

        assertFalse(exception.toString().contains(secret));
        assertNull(exception.getCause());
        server.verify();
    }

    private QuestionAssetStorageService service(
            RestClient.Builder builder,
            String secretKey,
            String legacyServiceRoleKey
    ) {
        QuestionAssetStorageService service = new QuestionAssetStorageService(builder);
        ReflectionTestUtils.setField(service, "supabaseUrl", SUPABASE_URL);
        ReflectionTestUtils.setField(service, "supabaseSecretKey", secretKey);
        ReflectionTestUtils.setField(service, "supabaseServiceRoleKey", legacyServiceRoleKey);
        ReflectionTestUtils.setField(service, "supabaseBucketQuestions", BUCKET);
        ReflectionTestUtils.setField(service, "localRoot", "unused");
        return service;
    }

    private void expectStorageRequests(
            MockRestServiceServer server,
            String expectedApiKey,
            String expectedAuthorization
    ) {
        server.expect(requestTo(SUPABASE_URL + "/storage/v1/bucket/" + BUCKET))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertHeaders(request.getHeaders(), expectedApiKey, expectedAuthorization))
                .andRespond(withSuccess());
        server.expect(requestTo(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/enem/question.png"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertHeaders(request.getHeaders(), expectedApiKey, expectedAuthorization))
                .andRespond(withSuccess());
    }

    private void assertHeaders(HttpHeaders headers, String expectedApiKey, String expectedAuthorization) {
        assertEquals(expectedApiKey, headers.getFirst("apikey"));
        assertEquals(expectedAuthorization, headers.getFirst(HttpHeaders.AUTHORIZATION));
    }
}
