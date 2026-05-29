package com.gabaritaplus.api.service.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
            restClientBuilder.build()
                    .post()
                    .uri(URI.create(baseUrl + "/storage/v1/object/" + supabaseBucketQuestions + "/" + encodedPath))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseServiceRoleKey)
                    .header("apikey", supabaseServiceRoleKey)
                    .header("x-upsert", "true")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();

            return new StoredAsset(
                    baseUrl + "/storage/v1/object/public/" + supabaseBucketQuestions + "/" + encodedPath,
                    encodedPath
            );
        }

        Path target = Path.of(localRoot).resolve(storagePath).normalize();
        Files.createDirectories(target.getParent());
        Files.write(target, content);
        return new StoredAsset(target.toUri().toString(), target.toString());
    }

    private boolean isSupabaseConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && supabaseServiceRoleKey != null && !supabaseServiceRoleKey.isBlank()
                && supabaseBucketQuestions != null && !supabaseBucketQuestions.isBlank();
    }

    public record StoredAsset(String publicUrl, String storagePath) {
    }
}
