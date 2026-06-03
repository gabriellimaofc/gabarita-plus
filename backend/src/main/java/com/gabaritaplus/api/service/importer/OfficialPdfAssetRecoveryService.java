package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.entity.OfficialExamSource;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.QuestionAsset;
import com.gabaritaplus.api.entity.enums.QuestionAssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialPdfAssetRecoveryService {

    private static final String BROKEN_IMAGE_URL = "https://enem.dev/broken-image.svg";
    private static final List<String> ENEM_2023_Q1_NEEDLES = List.of(
            "Quest\u00F5es de 01 a 05 (op\u00E7\u00E3o espanhol)",
            "\u00BFQU\u00C9 ME PASA?",
            "PORQU\u00C9 NO CONSIGO APRENDER",
            "Como estrellas en la tierra",
            "dislexia",
            "O filme Como estrellas en la tierra"
    );
    private static final List<String> SPANISH_MARKERS = List.of(
            "opcao espanhol",
            "opção espanhol",
            "espanhol",
            "¿",
            "porqué",
            "como estrellas en la tierra",
            "dislexia"
    );
    private static final List<String> ENGLISH_MARKERS = List.of(
            "opcao ingles",
            "opção ingles",
            "opção inglês",
            "ingles",
            "inglês",
            "english"
    );

    private final QuestionAssetStorageService storageService;

    @Value("${app.import.official-pdf-cache-dir:storage/official-pdfs}")
    private String pdfCacheDir;

    public OfficialPdfAssetRecoveryResult recover(Question question, OfficialExamSource source) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        LanguageOption questionLanguage = detectQuestionLanguageOption(question);
        OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics = OfficialPdfAssetRecoveryDiagnostics.builder()
                .recoveryAttempted(true)
                .officialSourceFound(source != null);
        diagnostics.languageOptionDetected(questionLanguage.name());

        if (source == null) {
            return fail(0, warnings, errors, diagnostics, "OFFICIAL_SOURCE_NOT_FOUND");
        }
        if (source.getPdfUrl() == null || source.getPdfUrl().isBlank()) {
            return fail(0, warnings, errors, diagnostics, "OFFICIAL_PDF_NOT_FOUND");
        }

        log.info(
                "Starting official PDF asset recovery questionId={} sourceQuestionNumber={} year={} day={} bookColor={} pdfUrl={}",
                question.getId(),
                question.getSourceQuestionNumber(),
                question.getSourceYear(),
                question.getSourceDay(),
                question.getSourceBookColor(),
                source.getPdfUrl()
        );

        try {
            Path pdfPath = resolveOfficialPdf(source, diagnostics);
            try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
                int pageCount = document.getNumberOfPages();
                diagnostics.pdfPageCount(pageCount);
                if (pageCount <= 0) {
                    return fail(0, warnings, errors, diagnostics, "PDF_EMPTY_OR_INVALID");
                }

                PageSelection selection = findQuestionPage(document, question);
                diagnostics.candidatePages(selection.candidatePages());
                diagnostics.rejectedCandidatePages(selection.rejectedCandidatePages());
                diagnostics.strongPhraseMatches(selection.strongPhraseMatches());
                diagnostics.selectedPageScore(selection.score());
                diagnostics.pageLanguageOptionDetected(selection.pageLanguageOption().name());
                if (selection.pageIndex() < 0) {
                    if (selection.wrongLanguageDetected()) {
                        warnings.add("WRONG_LANGUAGE_SECTION_DETECTED");
                    }
                    if (selection.ambiguous()) {
                        warnings.add("QUESTION_PAGE_AMBIGUOUS");
                    }
                    return fail(0, warnings, errors, diagnostics, "QUESTION_PAGE_NOT_FOUND");
                }

                int pageIndex = selection.pageIndex();
                diagnostics.selectedPage(pageIndex + 1).recoveryMethod(selection.method());
                log.info(
                        "Official PDF page selected questionId={} selectedPage={} method={} candidatePages={}",
                        question.getId(),
                        pageIndex + 1,
                        selection.method(),
                        selection.candidatePages()
                );

                BufferedImage pageImage = renderPage(document, pageIndex, question.getId(), diagnostics, errors, warnings);
                if (pageImage == null) {
                    return result(0, warnings, errors, diagnostics.recoveryFailureReason("PDF_RENDER_FAILED"));
                }

                Crop crop = determineCrop(pageImage, question, selection);
                diagnostics.cropMethod(crop.method())
                        .cropContainsMultipleQuestions(crop.containsMultipleQuestions())
                        .assetNeedsManualReview(true);
                BufferedImage cropImage = pageImage.getSubimage(crop.x(), crop.y(), crop.width(), crop.height());
                byte[] png = toPng(cropImage);
                String checksum = sha256(png);
                warnings.add("ASSET_RECOVERY_NEEDS_REVIEW");
                if (selection.wrongLanguageDetected()) {
                    warnings.add("WRONG_LANGUAGE_SECTION_DETECTED");
                }
                if (selection.ambiguous() || crop.containsMultipleQuestions()) {
                    warnings.add("QUESTION_PAGE_AMBIGUOUS");
                }

                if (hasAssetWithChecksum(question, checksum)) {
                    warnings.add("ASSET_RECOVERED_FROM_OFFICIAL_PDF");
                    clearBrokenImageReferences(question);
                    return result(0, warnings, errors, diagnostics.recoveryMethod(selection.method()));
                }

                if (!storageService.isSupabaseConfigured()) {
                    diagnostics.storageUploadAttempted(false).storageUploadSuccess(false);
                    return fail(0, warnings, errors, diagnostics, "STORAGE_NOT_CONFIGURED");
                }

                String storagePath = buildStoragePath(question, checksum);
                diagnostics.storageUploadAttempted(true);
                QuestionAssetStorageService.StoredAsset storedAsset = storeOfficialAsset(
                        storagePath,
                        png,
                        question.getId(),
                        diagnostics,
                        errors
                );
                if (storedAsset == null) {
                    return result(0, warnings, errors, diagnostics);
                }

                try {
                    replaceRecoverableOfficialAssets(question);
                    QuestionAsset asset = buildAsset(question, storedAsset, storagePath, pageIndex, crop, checksum);
                    question.getAssets().add(asset);
                    question.setOfficialPage(pageIndex + 1);
                    clearBrokenImageReferences(question);
                    warnings.add("ASSET_RECOVERED_FROM_OFFICIAL_PDF");
                } catch (Exception exception) {
                    log.warn("Official PDF asset entity save preparation failed questionId={} reason={}", question.getId(), exception.getMessage());
                    return fail(0, warnings, errors, diagnostics, "ASSET_ENTITY_SAVE_FAILED");
                }

                log.info(
                        "Official PDF asset recovered questionId={} selectedPage={} crop={}x{}+{}+{} storagePath={} url={}",
                        question.getId(),
                        pageIndex + 1,
                        crop.width(),
                        crop.height(),
                        crop.x(),
                        crop.y(),
                        storedAsset.storagePath(),
                        storedAsset.publicUrl()
                );
                return result(1, warnings, errors, diagnostics);
            }
        } catch (PdfRecoveryException exception) {
            log.warn("Official PDF asset recovery failed questionId={} reason={}", question.getId(), exception.reason());
            return fail(0, warnings, errors, diagnostics, exception.reason());
        } catch (Exception exception) {
            log.warn("Official PDF asset recovery failed questionId={} reason={}", question.getId(), exception.getMessage());
            return fail(0, warnings, errors, diagnostics, "UNKNOWN_RECOVERY_ERROR");
        }
    }

    private BufferedImage renderPage(
            PDDocument document,
            int pageIndex,
            Long questionId,
            OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics,
            List<String> errors,
            List<String> warnings
    ) {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 180, ImageType.RGB);
            diagnostics.pdfRendered(true)
                    .renderedWidth(pageImage.getWidth())
                    .renderedHeight(pageImage.getHeight());
            log.info(
                    "Official PDF page rendered questionId={} selectedPage={} width={} height={}",
                    questionId,
                    pageIndex + 1,
                    pageImage.getWidth(),
                    pageImage.getHeight()
            );
            return pageImage;
        } catch (Exception exception) {
            errors.add("PDF_RENDER_FAILED");
            warnings.add("ASSET_RECOVERY_FAILED");
            log.warn("Official PDF render failed questionId={} selectedPage={} reason={}", questionId, pageIndex + 1, exception.getMessage());
            return null;
        }
    }

    private QuestionAssetStorageService.StoredAsset storeOfficialAsset(
            String storagePath,
            byte[] png,
            Long questionId,
            OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics,
            List<String> errors
    ) throws Exception {
        try {
            QuestionAssetStorageService.StoredAsset storedAsset = storageService.storePng(storagePath, png);
            diagnostics.storageUploadSuccess(true).assetUrl(storedAsset.publicUrl());
            log.info("Official PDF asset upload completed questionId={} storagePath={} url={}", questionId, storedAsset.storagePath(), storedAsset.publicUrl());
            return storedAsset;
        } catch (QuestionAssetStorageException exception) {
            errors.add(exception.reason());
            diagnostics.storageUploadSuccess(false).recoveryFailureReason(exception.reason());
            log.warn("Official PDF asset storage failed questionId={} reason={}", questionId, exception.reason());
            return null;
        }
    }

    private OfficialPdfAssetRecoveryResult fail(
            int recoveredAssets,
            List<String> warnings,
            List<String> errors,
            OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics,
            String reason
    ) {
        if (!errors.contains(reason)) {
            errors.add(reason);
        }
        if (!warnings.contains("ASSET_RECOVERY_FAILED")) {
            warnings.add("ASSET_RECOVERY_FAILED");
        }
        return result(recoveredAssets, warnings, errors, diagnostics.recoveryFailureReason(reason));
    }

    private OfficialPdfAssetRecoveryResult result(
            int recoveredAssets,
            List<String> warnings,
            List<String> errors,
            OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics
    ) {
        return new OfficialPdfAssetRecoveryResult(
                recoveredAssets,
                List.copyOf(warnings.stream().distinct().toList()),
                List.copyOf(errors.stream().distinct().toList()),
                diagnostics.build()
        );
    }

    private Path resolveOfficialPdf(OfficialExamSource source, OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics) {
        try {
            Path cacheDirectory = Path.of(pdfCacheDir);
            Files.createDirectories(cacheDirectory);
            String fileName = source.getYear() + "_D" + source.getDay() + "_" + source.getBookColor() + ".pdf";
            Path cachedPdf = cacheDirectory.resolve(fileName.replaceAll("[^A-Za-z0-9_.-]", "_"));
            if (Files.exists(cachedPdf) && Files.size(cachedPdf) > 0) {
                diagnostics.pdfDownloaded(true).pdfSizeBytes(Files.size(cachedPdf));
                diagnostics.pdfDownloadSource("LOCAL_CACHE");
                log.info("Official PDF loaded from generated local cache path={} bytes={}", cachedPdf, Files.size(cachedPdf));
                return cachedPdf;
            }

            PdfRecoveryException officialFailure = null;
            if (source.getPdfUrl() != null && !source.getPdfUrl().isBlank()) {
                try {
                    byte[] pdf = downloadOfficialPdf(source.getPdfUrl(), "OFFICIAL_URL", diagnostics);
                    Files.write(cachedPdf, pdf);
                    diagnostics.pdfDownloaded(true).pdfSizeBytes((long) pdf.length);
                    log.info("Official PDF downloaded from official URL pdfUrl={} bytes={}", source.getPdfUrl(), pdf.length);
                    return cachedPdf;
                } catch (PdfRecoveryException exception) {
                    officialFailure = exception;
                    diagnostics.sslFailure(diagnostics.sslFailure() || isSslFailure(exception.getMessage()));
                    log.warn("Official PDF URL download failed pdfUrl={} reason={} sslFailure={}", source.getPdfUrl(), exception.getMessage(), diagnostics.sslFailure());
                }
            }

            Path localFallback = resolveLocalPdfFallback(source, diagnostics);
            if (localFallback != null) {
                return localFallback;
            }

            if (source.getCachedPdfUrl() != null && !source.getCachedPdfUrl().isBlank()) {
                Path cachedUrlPdf = cacheDirectory.resolve(("cached_" + fileName).replaceAll("[^A-Za-z0-9_.-]", "_"));
                byte[] pdf = downloadOfficialPdf(source.getCachedPdfUrl(), "CACHED_URL", diagnostics);
                Files.write(cachedUrlPdf, pdf);
                diagnostics.pdfDownloaded(true)
                        .pdfSizeBytes((long) pdf.length)
                        .cachedPdfUrlUsed(source.getCachedPdfUrl());
                log.info("Official PDF downloaded from cached URL cachedPdfUrl={} bytes={}", source.getCachedPdfUrl(), pdf.length);
                return cachedUrlPdf;
            }

            if (officialFailure != null) {
                throw officialFailure;
            }
            throw new PdfRecoveryException("PDF_DOWNLOAD_FAILED", "No official or cached PDF URL was available.");
        } catch (PdfRecoveryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PdfRecoveryException("PDF_DOWNLOAD_FAILED", exception);
        }
    }

    private Path resolveLocalPdfFallback(OfficialExamSource source, OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics) throws Exception {
        if (source.getLocalPdfPath() == null || source.getLocalPdfPath().isBlank()) {
            return null;
        }
        Path localPath = Path.of(source.getLocalPdfPath());
        if (!Files.exists(localPath) || Files.size(localPath) <= 0) {
            diagnostics.pdfDownloadErrorMessage("localPdfPath not found or empty: " + source.getLocalPdfPath());
            return null;
        }
        byte[] bytes = Files.readAllBytes(localPath);
        validatePdfResponse(200, "application/pdf", bytes);
        diagnostics.pdfDownloaded(true)
                .pdfSizeBytes(Files.size(localPath))
                .pdfDownloadHttpStatus(200)
                .pdfDownloadContentType("application/pdf")
                .pdfDownloadContentLength(Files.size(localPath))
                .pdfUrlUsed(source.getLocalPdfPath())
                .cachedPdfUrlUsed(source.getLocalPdfPath())
                .pdfDownloadSource("LOCAL_PDF_PATH");
        log.info("Official PDF loaded from localPdfPath path={} bytes={}", localPath, Files.size(localPath));
        return localPath;
    }

    private byte[] downloadOfficialPdf(String pdfUrl, String sourceType, OfficialPdfAssetRecoveryDiagnostics.Builder diagnostics) {
        diagnostics.pdfUrlUsed(pdfUrl);
        if ("OFFICIAL_URL".equals(sourceType)) {
            diagnostics.attemptedOfficialPdfUrl(true);
        }
        if ("CACHED_URL".equals(sourceType)) {
            diagnostics.attemptedCachedPdfUrl(true).cachedPdfUrlUsed(pdfUrl);
        }
        List<DownloadAttempt> attempts = List.of(
                new DownloadAttempt("DEFAULT_BROWSER_HEADERS", true),
                new DownloadAttempt("IDENTITY_ENCODING", false)
        );

        PdfRecoveryException lastFailure = null;
        for (DownloadAttempt attempt : attempts) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(pdfUrl))
                        .timeout(Duration.ofSeconds(90))
                        .GET()
                        .header("User-Agent", "Mozilla/5.0 (compatible; GabaritaPlusBot/1.0; +https://gabarita-plus.vercel.app)")
                        .header("Accept", "application/pdf,application/octet-stream;q=0.9,*/*;q=0.1")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache");
                if (!attempt.allowCompression()) {
                    requestBuilder.header("Accept-Encoding", "identity");
                }

                HttpResponse<byte[]> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
                byte[] body = response.body();
                String contentType = response.headers().firstValue("content-type").orElse(null);
                long contentLength = response.headers()
                        .firstValue("content-length")
                        .flatMap(this::parseLong)
                        .orElse(body == null ? 0L : (long) body.length);

                diagnostics.pdfDownloadHttpStatus(response.statusCode())
                        .pdfDownloadContentType(contentType)
                        .pdfDownloadContentLength(contentLength)
                        .pdfDownloadErrorMessage(null)
                        .pdfDownloadSource(sourceType);

                log.info(
                        "Official PDF download attempt method={} status={} contentType={} contentLength={} url={}",
                        attempt.name(),
                        response.statusCode(),
                        contentType,
                        contentLength,
                        pdfUrl
                );

                validatePdfResponse(response.statusCode(), contentType, body);
                return body;
            } catch (PdfRecoveryException exception) {
                lastFailure = exception;
                diagnostics.sslFailure(diagnostics.sslFailure() || isSslFailure(exception.getMessage()));
                diagnostics.pdfDownloadErrorMessage(exception.getMessage());
                log.warn("Official PDF download attempt failed method={} reason={}", attempt.name(), exception.getMessage());
            } catch (Exception exception) {
                lastFailure = new PdfRecoveryException("PDF_DOWNLOAD_FAILED", sanitizeDownloadError(exception));
                diagnostics.sslFailure(diagnostics.sslFailure() || isSslFailure(lastFailure.getMessage()));
                diagnostics.pdfDownloadErrorMessage(lastFailure.getMessage());
                log.warn("Official PDF download attempt failed method={} reason={}", attempt.name(), sanitizeDownloadError(exception));
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new PdfRecoveryException("PDF_DOWNLOAD_FAILED", "No download attempts were executed.");
    }

    private boolean isSslFailure(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("pkix")
                || normalized.contains("certificate_unknown")
                || normalized.contains("certpath")
                || normalized.contains("sslhandshakeexception")
                || normalized.contains("unable to find valid certification path");
    }

    private void validatePdfResponse(int statusCode, String contentType, byte[] body) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new PdfRecoveryException("PDF_DOWNLOAD_FAILED", "HTTP status " + statusCode);
        }
        if (body == null || body.length == 0) {
            throw new PdfRecoveryException("PDF_EMPTY_OR_INVALID", "PDF download returned empty content.");
        }
        if (body.length < 1024) {
            throw new PdfRecoveryException("PDF_EMPTY_OR_INVALID", "PDF download returned too few bytes: " + body.length);
        }
        if (contentType != null && !contentType.isBlank()) {
            String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
            boolean acceptedType = normalizedContentType.contains("application/pdf")
                    || normalizedContentType.contains("application/octet-stream")
                    || normalizedContentType.contains("binary/octet-stream");
            if (!acceptedType) {
                throw new PdfRecoveryException("PDF_EMPTY_OR_INVALID", "Unexpected PDF content-type: " + contentType);
            }
        }
        if (!startsWithPdfHeader(body)) {
            throw new PdfRecoveryException("PDF_EMPTY_OR_INVALID", "Downloaded bytes do not start with %PDF.");
        }
    }

    private boolean startsWithPdfHeader(byte[] body) {
        if (body.length < 4) {
            return false;
        }
        int offset = 0;
        if (body.length >= 7
                && (body[0] & 0xFF) == 0xEF
                && (body[1] & 0xFF) == 0xBB
                && (body[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        return body.length >= offset + 4
                && body[offset] == '%'
                && body[offset + 1] == 'P'
                && body[offset + 2] == 'D'
                && body[offset + 3] == 'F';
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String sanitizeDownloadError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private PageSelection findQuestionPage(PDDocument document, Question question) throws Exception {
        Set<Integer> candidatePages = new LinkedHashSet<>();
        List<String> rejectedCandidatePages = new ArrayList<>();
        List<String> needles = buildNeedles(question);
        List<String> strongPhrases = buildStrongPhrases(question);
        LanguageOption expectedLanguage = detectQuestionLanguageOption(question);
        PDFTextStripper stripper = new PDFTextStripper();
        List<PageCandidate> matches = new ArrayList<>();

        log.info("Official PDF text search questionId={} needles={} strongPhrases={} expectedLanguage={}",
                question.getId(), needles, strongPhrases, expectedLanguage);
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String rawText = stripper.getText(document);
            String text = normalizeSearchText(rawText);
            PageCandidate candidate = scorePage(page, text, question, needles, strongPhrases, expectedLanguage);
            if (candidate.questionMarkerFound() || !candidate.strongPhraseMatches().isEmpty() || candidate.score() > 0) {
                candidatePages.add(page);
            }
            if (candidate.rejectedReason() != null) {
                rejectedCandidatePages.add("p." + page + ": " + candidate.rejectedReason());
            }
            if (candidate.score() > 0 && candidate.rejectedReason() == null) {
                matches.add(candidate);
            }
        }

        if (!matches.isEmpty()) {
            PageCandidate best = matches.stream()
                    .max(Comparator.comparingInt(PageCandidate::score)
                            .thenComparingInt(candidate -> candidate.strongPhraseMatches().size()))
                    .orElseThrow();
            String method = best.strongPhraseMatches().isEmpty() ? "QUESTION_NUMBER_AND_LANGUAGE_MATCH" : "TEXT_MATCH";
            return new PageSelection(
                    best.page() - 1,
                    List.copyOf(candidatePages),
                    rejectedCandidatePages,
                    method,
                    best.pageLanguage(),
                    best.strongPhraseMatches(),
                    best.score(),
                    best.wrongLanguageDetected(),
                    best.ambiguous()
            );
        }

        List<Integer> fallbackPages = fallbackPages(question, document.getNumberOfPages());
        candidatePages.addAll(fallbackPages);
        if (!fallbackPages.isEmpty()) {
            return new PageSelection(
                    fallbackPages.get(0) - 1,
                    List.copyOf(candidatePages),
                    rejectedCandidatePages,
                    "FALLBACK_WIDE_CROP",
                    LanguageOption.UNKNOWN,
                    List.of(),
                    0,
                    rejectedCandidatePages.stream().anyMatch(item -> item.contains("WRONG_LANGUAGE_SECTION_DETECTED")),
                    true
            );
        }

        return new PageSelection(
                -1,
                List.copyOf(candidatePages),
                rejectedCandidatePages,
                "NO_PAGE_FOUND",
                LanguageOption.UNKNOWN,
                List.of(),
                0,
                rejectedCandidatePages.stream().anyMatch(item -> item.contains("WRONG_LANGUAGE_SECTION_DETECTED")),
                true
        );
    }

    private List<String> buildNeedles(Question question) {
        List<String> needles = new ArrayList<>();
        Integer number = question.getSourceQuestionNumber();
        if (number != null) {
            String padded = String.format(Locale.ROOT, "%02d", number);
            needles.add("QUEST\u00C3O " + padded);
            needles.add("QUESTAO " + padded);
            needles.add("Quest\u00E3o " + number);
            needles.add("QUEST\u00C3O " + number);
        }
        if (question.getSourceYear() != null && question.getSourceYear() == 2023
                && question.getSourceQuestionNumber() != null && question.getSourceQuestionNumber() == 1) {
            needles.addAll(ENEM_2023_Q1_NEEDLES);
        }
        return needles;
    }

    private List<String> buildStrongPhrases(Question question) {
        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        if (question.getStatement() != null) {
            String[] parts = question.getStatement().split("[\\n\\r.!?]+");
            for (String part : parts) {
                String normalized = part.trim();
                if (normalized.length() >= 10) {
                    phrases.add(normalized);
                }
            }
        }
        if (question.getSourceYear() != null
                && question.getSourceYear() == 2023
                && question.getSourceQuestionNumber() != null
                && question.getSourceQuestionNumber() == 1
                && detectQuestionLanguageOption(question) == LanguageOption.ESPANHOL) {
            phrases.add("\u00BFQU\u00C9 ME PASA?");
            phrases.add("PORQU\u00C9 NO CONSIGO APRENDER");
            phrases.add("Como estrellas en la tierra");
            phrases.add("dislexia");
        }
        return phrases.stream().limit(8).toList();
    }

    private PageCandidate scorePage(
            int page,
            String normalizedText,
            Question question,
            List<String> needles,
            List<String> strongPhrases,
            LanguageOption expectedLanguage
    ) {
        Integer number = question.getSourceQuestionNumber();
        String padded = number == null ? null : String.format(Locale.ROOT, "%02d", number);
        boolean questionMarkerFound = padded != null && (
                normalizedText.contains("QUESTAO " + padded)
                        || normalizedText.contains("QUESTAO " + number)
        );
        LanguageOption pageLanguage = detectPageLanguageOption(normalizedText);
        List<String> strongMatches = strongPhrases.stream()
                .map(this::normalizeSearchText)
                .filter(value -> !value.isBlank() && normalizedText.contains(value))
                .distinct()
                .toList();

        int score = 0;
        if (questionMarkerFound) {
            score += 20;
        }
        if (expectedLanguage != LanguageOption.UNKNOWN && pageLanguage == expectedLanguage) {
            score += 40;
        }
        if (!strongMatches.isEmpty()) {
            score += Math.min(60, strongMatches.size() * 25);
        }
        for (String needle : needles) {
            String normalizedNeedle = normalizeSearchText(needle);
            if (!normalizedNeedle.isBlank() && normalizedText.contains(normalizedNeedle)) {
                score += 10;
            }
        }

        boolean wrongLanguage = expectedLanguage != LanguageOption.UNKNOWN
                && pageLanguage != LanguageOption.UNKNOWN
                && pageLanguage != expectedLanguage;
        boolean ambiguous = questionMarkerFound && strongMatches.isEmpty();

        if (wrongLanguage) {
            return new PageCandidate(
                    page,
                    0,
                    pageLanguage,
                    strongMatches,
                    questionMarkerFound,
                    "WRONG_LANGUAGE_SECTION_DETECTED",
                    true,
                    true
            );
        }

        if (expectedLanguage != LanguageOption.UNKNOWN && strongMatches.isEmpty() && !questionMarkerFound) {
            return new PageCandidate(
                    page,
                    0,
                    pageLanguage,
                    List.of(),
                    false,
                    "QUESTION_PAGE_AMBIGUOUS",
                    false,
                    true
            );
        }

        return new PageCandidate(
                page,
                score,
                pageLanguage,
                strongMatches,
                questionMarkerFound,
                null,
                false,
                ambiguous
        );
    }

    private LanguageOption detectQuestionLanguageOption(Question question) {
        String content = String.join(
                " ",
                String.valueOf(question.getSubject()),
                String.valueOf(question.getTopic()),
                String.valueOf(question.getSubtopic()),
                String.valueOf(question.getTitle()),
                String.valueOf(question.getStatement())
        );
        String normalized = normalizeSearchText(content);
        if (SPANISH_MARKERS.stream().map(this::normalizeSearchText).anyMatch(normalized::contains)) {
            return LanguageOption.ESPANHOL;
        }
        if (ENGLISH_MARKERS.stream().map(this::normalizeSearchText).anyMatch(normalized::contains)) {
            return LanguageOption.INGLES;
        }
        return LanguageOption.UNKNOWN;
    }

    private LanguageOption detectPageLanguageOption(String normalizedText) {
        boolean spanish = SPANISH_MARKERS.stream()
                .map(this::normalizeSearchText)
                .anyMatch(normalizedText::contains);
        boolean english = ENGLISH_MARKERS.stream()
                .map(this::normalizeSearchText)
                .anyMatch(normalizedText::contains);
        if (spanish && !english) {
            return LanguageOption.ESPANHOL;
        }
        if (english && !spanish) {
            return LanguageOption.INGLES;
        }
        return LanguageOption.UNKNOWN;
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<Integer> fallbackPages(Question question, int pageCount) {
        if (question.getOfficialPage() != null && question.getOfficialPage() > 0 && question.getOfficialPage() <= pageCount) {
            return List.of(question.getOfficialPage());
        }
        if (question.getSourcePage() != null && question.getSourcePage() > 0 && question.getSourcePage() <= pageCount) {
            return List.of(question.getSourcePage());
        }
        if (question.getSourceQuestionNumber() != null && question.getSourceQuestionNumber() == 1) {
            if (question.getSourceYear() != null && question.getSourceYear() == 2023
                    && question.getSourceDay() != null && question.getSourceDay() == 1
                    && question.getSourceBookColor() != null && question.getSourceBookColor().equalsIgnoreCase("AZUL")
                    && pageCount >= 4) {
                return List.of(4, 3, 2, 5, 6).stream()
                        .filter(page -> page > 0 && page <= pageCount)
                        .distinct()
                        .toList();
            }
            List<Integer> pages = new ArrayList<>();
            for (int page = 2; page <= Math.min(pageCount, 6); page++) {
                pages.add(page);
            }
            return pages;
        }
        int questionNumber = question.getSourceQuestionNumber() == null ? 1 : question.getSourceQuestionNumber();
        return List.of(Math.min(pageCount, Math.max(1, 2 + (questionNumber - 1) / 3)));
    }

    private Crop determineCrop(BufferedImage image, Question question, PageSelection selection) {
        if (selection.method() != null && selection.method().startsWith("FALLBACK")) {
            return new Crop(0, 0, image.getWidth(), image.getHeight(), "FULL_PAGE", true);
        }
        int marginX = Math.max(24, image.getWidth() / 25);
        int top = Math.max(24, image.getHeight() / 18);
        int height = (int) Math.round(image.getHeight() * 0.72);
        if (question.getSourceQuestionNumber() != null && question.getSourceQuestionNumber() > 1) {
            top = Math.max(24, image.getHeight() / 8);
            height = (int) Math.round(image.getHeight() * 0.68);
        }
        int width = image.getWidth() - (marginX * 2);
        height = Math.min(height, image.getHeight() - top - 24);
        if (!selection.strongPhraseMatches().isEmpty() && question.getSourceQuestionNumber() != null
                && question.getSourceQuestionNumber() == 1) {
            int focusedHeight = Math.min(height, (int) Math.round(image.getHeight() * 0.52));
            return new Crop(marginX, top, width, focusedHeight, "PAGE_CROP", true);
        }
        return new Crop(marginX, top, width, height, "PAGE_CROP", true);
    }

    private byte[] toPng(BufferedImage image) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private String sha256(byte[] value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value));
    }

    private boolean hasAssetWithChecksum(Question question, String checksum) {
        return question.getAssets().stream()
                .map(QuestionAsset::getChecksum)
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(checksum::equalsIgnoreCase);
    }

    private void replaceRecoverableOfficialAssets(Question question) {
        question.getAssets().removeIf(asset -> isRecoverableOfficialAsset(question, asset));
    }

    private boolean isRecoverableOfficialAsset(Question question, QuestionAsset asset) {
        if (question.getImportStatus() == com.gabaritaplus.api.entity.enums.QuestionImportStatus.PUBLISHED) {
            return false;
        }
        String storagePath = asset.getStoragePath() == null ? "" : asset.getStoragePath().toLowerCase(Locale.ROOT);
        String caption = asset.getCaption() == null ? "" : asset.getCaption().toLowerCase(Locale.ROOT);
        return storagePath.contains("official-pdf-")
                || caption.contains("pdf oficial")
                || caption.contains("revisão humana obrigatória")
                || caption.contains("revisao humana obrigatoria");
    }

    private String buildStoragePath(Question question, String checksum) {
        String color = question.getSourceBookColor() == null ? "UNKNOWN" : question.getSourceBookColor();
        return "enem/%d/dia-%s/%s/q%s/official-pdf-%s.png".formatted(
                question.getSourceYear(),
                question.getSourceDay(),
                color.toLowerCase(Locale.ROOT),
                question.getSourceQuestionNumber(),
                checksum.substring(0, 16)
        );
    }

    private QuestionAsset buildAsset(
            Question question,
            QuestionAssetStorageService.StoredAsset storedAsset,
            String storagePath,
            int pageIndex,
            Crop crop,
            String checksum
    ) {
        QuestionAsset asset = new QuestionAsset();
        asset.setQuestion(question);
        asset.setType(QuestionAssetType.IMAGE);
        asset.setUrl(storedAsset.publicUrl());
        asset.setStoragePath(storedAsset.storagePath());
        asset.setOriginalFileName(Path.of(storagePath).getFileName().toString());
        asset.setSourcePage(pageIndex + 1);
        asset.setCropX(crop.x());
        asset.setCropY(crop.y());
        asset.setCropWidth(crop.width());
        asset.setCropHeight(crop.height());
        asset.setAltText("Recorte oficial do PDF do INEP para a quest\u00E3o " + question.getSourceQuestionNumber() + ".");
        asset.setCaption(
                "QUESTION_BLOCK".equals(crop.method())
                        ? "Recorte da questão recuperado do PDF oficial do INEP. Revisão humana recomendada antes da publicação."
                        : "Recorte amplo recuperado do PDF oficial do INEP. Revisão humana obrigatória antes da publicação."
        );
        asset.setChecksum(checksum);
        return asset;
    }

    private void clearBrokenImageReferences(Question question) {
        question.setImageUrl(removeBrokenImage(question.getImageUrl()));
        question.setStatement(removeBrokenImage(question.getStatement()));
        question.setStatementHtml(removeBrokenImage(question.getStatementHtml()));
    }

    private String removeBrokenImage(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("(?i)<img[^>]+src=[\"']" + java.util.regex.Pattern.quote(BROKEN_IMAGE_URL) + "[\"'][^>]*>", "[Imagem oficial recuperada nos recursos da quest\u00E3o.]")
                .replace("![](" + BROKEN_IMAGE_URL + ")", "[Imagem oficial recuperada nos recursos da quest\u00E3o.]")
                .replace("![ ](" + BROKEN_IMAGE_URL + ")", "[Imagem oficial recuperada nos recursos da quest\u00E3o.]")
                .replace(BROKEN_IMAGE_URL, "");
    }

    private record PageSelection(
            int pageIndex,
            List<Integer> candidatePages,
            List<String> rejectedCandidatePages,
            String method,
            LanguageOption pageLanguageOption,
            List<String> strongPhraseMatches,
            int score,
            boolean wrongLanguageDetected,
            boolean ambiguous
    ) {
    }

    private record PageCandidate(
            int page,
            int score,
            LanguageOption pageLanguage,
            List<String> strongPhraseMatches,
            boolean questionMarkerFound,
            String rejectedReason,
            boolean wrongLanguageDetected,
            boolean ambiguous
    ) {
    }

    private record Crop(int x, int y, int width, int height, String method, boolean containsMultipleQuestions) {
    }

    private record DownloadAttempt(String name, boolean allowCompression) {
    }

    private static class PdfRecoveryException extends RuntimeException {
        private final String reason;

        PdfRecoveryException(String reason) {
            super(reason);
            this.reason = reason;
        }

        PdfRecoveryException(String reason, Throwable cause) {
            super(reason, cause);
            this.reason = reason;
        }

        PdfRecoveryException(String reason, String message) {
            super(message);
            this.reason = reason;
        }

        String reason() {
            return reason;
        }
    }

    private enum LanguageOption {
        ESPANHOL,
        INGLES,
        UNKNOWN
    }
}
