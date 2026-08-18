package com.gabaritaplus.api.service.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class QuestionAssetStorageService {

    private final RestClient.Builder restClientBuilder;

    @Value("${SUPABASE_URL:${supabase.url:}}")
    private String supabaseUrl;

    @Value("${SUPABASE_SECRET_KEY:${supabase.secret-key:}}")
    private String supabaseSecretKey;

    @Value("${SUPABASE_SERVICE_ROLE_KEY:${supabase.service-role-key:}}")
    private String supabaseServiceRoleKey;

    @Value("${SUPABASE_BUCKET_QUESTIONS:${supabase.bucket-questions:questions}}")
    private String supabaseBucketQuestions;

    @Value("${app.storage.local-root:storage/questions}")
    private String localRoot;

    public StoredAsset storePng(String storagePath, byte[] content) throws IOException {
        if (isSupabaseConfigured()) {
            String baseUrl = supabaseUrl.replaceAll("/+$", "");
            String encodedPath = storagePath.replace("\\", "/");
            String apiKey = effectiveSupabaseApiKey();
            RestClient restClient = restClientBuilder.build();
            try {
                restClient
                        .get()
                        .uri(URI.create(baseUrl + "/storage/v1/bucket/" + supabaseBucketQuestions))
                        .headers(headers -> headers.addAll(supabaseHeaders(apiKey)))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.NotFound exception) {
                throw new QuestionAssetStorageException("STORAGE_BUCKET_NOT_FOUND", "Supabase bucket not found.");
            } catch (RestClientException exception) {
                throw new QuestionAssetStorageException("STORAGE_UPLOAD_FAILED", "Could not validate Supabase bucket.");
            }

            try {
                ResponseEntity<Void> response = restClient
                        .post()
                        .uri(URI.create(baseUrl + "/storage/v1/object/" + supabaseBucketQuestions + "/" + encodedPath))
                        .headers(headers -> headers.addAll(supabaseHeaders(apiKey)))
                        .header("x-upsert", "true")
                        .contentType(MediaType.IMAGE_PNG)
                        .body(content)
                        .retrieve()
                        .toBodilessEntity();
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new QuestionAssetStorageException("STORAGE_UPLOAD_FAILED", "Supabase upload returned non-success status.");
                }
            } catch (QuestionAssetStorageException exception) {
                throw exception;
            } catch (HttpClientErrorException.NotFound exception) {
                throw new QuestionAssetStorageException("STORAGE_BUCKET_NOT_FOUND", "Supabase bucket not found during upload.");
            } catch (RestClientException exception) {
                throw new QuestionAssetStorageException("STORAGE_UPLOAD_FAILED", "Supabase upload failed.");
            }

            return new StoredAsset(
                    baseUrl + "/storage/v1/object/public/" + supabaseBucketQuestions + "/" + encodedPath,
                    encodedPath
            );
        }

        Path target = Path.of(localRoot).resolve(storagePath).normalize();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, content);
        return new StoredAsset(target.toUri().toString(), target.toString());
    }

    public boolean isSupabaseConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && effectiveSupabaseApiKey() != null
                && supabaseBucketQuestions != null && !supabaseBucketQuestions.isBlank();
    }

    private String effectiveSupabaseApiKey() {
        if (supabaseSecretKey != null && !supabaseSecretKey.isBlank()) {
            return supabaseSecretKey;
        }
        if (supabaseServiceRoleKey != null && !supabaseServiceRoleKey.isBlank()) {
            return supabaseServiceRoleKey;
        }
        return null;
    }

    private HttpHeaders supabaseHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        if (!apiKey.startsWith("sb_secret_")) {
            headers.setBearerAuth(apiKey);
        }
        return headers;
    }

    public record StoredAsset(String publicUrl, String storagePath) {
    }
}
