package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.entity.OfficialExamSource;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.QuestionAsset;
import com.gabaritaplus.api.entity.enums.QuestionAssetType;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OfficialPdfAssetRecoveryService {

    private static final String BROKEN_IMAGE_URL = "https://enem.dev/broken-image.svg";

    private final QuestionAssetStorageService storageService;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.import.official-pdf-cache-dir:storage/official-pdfs}")
    private String pdfCacheDir;

    public OfficialPdfAssetRecoveryResult recover(Question question, OfficialExamSource source) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (source.getPdfUrl() == null || source.getPdfUrl().isBlank()) {
            errors.add("OFFICIAL_PDF_NOT_FOUND");
            return new OfficialPdfAssetRecoveryResult(0, warnings, errors);
        }

        try {
            Path pdfPath = resolveOfficialPdf(source);
            try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
                int pageIndex = findQuestionPage(document, question);
                if (pageIndex < 0) {
                    errors.add("QUESTION_PAGE_NOT_FOUND");
                    return new OfficialPdfAssetRecoveryResult(0, warnings, errors);
                }

                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 180, ImageType.RGB);
                Crop crop = broadQuestionCrop(pageImage, question);
                BufferedImage cropImage = pageImage.getSubimage(crop.x(), crop.y(), crop.width(), crop.height());

                byte[] png = toPng(cropImage);
                String checksum = sha256(png);
                if (hasAssetWithChecksum(question, checksum)) {
                    warnings.add("ASSET_RECOVERED_FROM_OFFICIAL_PDF");
                    warnings.add("ASSET_RECOVERY_NEEDS_REVIEW");
                    clearBrokenImageReferences(question);
                    return new OfficialPdfAssetRecoveryResult(0, warnings, errors);
                }

                String storagePath = buildStoragePath(question, checksum);
                QuestionAssetStorageService.StoredAsset storedAsset = storageService.storePng(storagePath, png);
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
                asset.setAltText("Recorte oficial do PDF do INEP para a questão " + question.getSourceQuestionNumber() + ".");
                asset.setCaption("Recorte recuperado do PDF oficial do INEP. Revisão humana recomendada antes da publicação.");
                asset.setChecksum(checksum);
                question.getAssets().add(asset);

                question.setOfficialPage(pageIndex + 1);
                clearBrokenImageReferences(question);
                warnings.add("ASSET_RECOVERED_FROM_OFFICIAL_PDF");
                warnings.add("ASSET_RECOVERY_NEEDS_REVIEW");
                return new OfficialPdfAssetRecoveryResult(1, warnings, errors);
            }
        } catch (Exception exception) {
            warnings.add("ASSET_RECOVERY_FAILED");
            return new OfficialPdfAssetRecoveryResult(0, warnings, errors);
        }
    }

    private Path resolveOfficialPdf(OfficialExamSource source) throws Exception {
        if (source.getLocalPdfPath() != null && !source.getLocalPdfPath().isBlank()) {
            Path localPath = Path.of(source.getLocalPdfPath());
            if (Files.exists(localPath)) {
                return localPath;
            }
        }

        Path cacheDirectory = Path.of(pdfCacheDir);
        Files.createDirectories(cacheDirectory);
        String fileName = source.getYear() + "_D" + source.getDay() + "_" + source.getBookColor() + ".pdf";
        Path cachedPdf = cacheDirectory.resolve(fileName.replaceAll("[^A-Za-z0-9_.-]", "_"));
        if (Files.exists(cachedPdf) && Files.size(cachedPdf) > 0) {
            return cachedPdf;
        }

        byte[] pdf = restClientBuilder.build()
                .get()
                .uri(source.getPdfUrl())
                .retrieve()
                .body(byte[].class);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Official PDF download returned empty content.");
        }
        Files.write(cachedPdf, pdf);
        return cachedPdf;
    }

    private int findQuestionPage(PDDocument document, Question question) throws Exception {
        if (question.getOfficialPage() != null && question.getOfficialPage() > 0
                && question.getOfficialPage() <= document.getNumberOfPages()) {
            return question.getOfficialPage() - 1;
        }
        if (question.getSourcePage() != null && question.getSourcePage() > 0
                && question.getSourcePage() <= document.getNumberOfPages()) {
            return question.getSourcePage() - 1;
        }

        Integer number = question.getSourceQuestionNumber();
        if (number == null) {
            return -1;
        }

        PDFTextStripper stripper = new PDFTextStripper();
        String padded = String.format(Locale.ROOT, "%02d", number);
        List<String> needles = List.of(
                "QUESTÃO " + padded,
                "QUESTAO " + padded,
                "Questão " + number,
                "QUESTÃO " + number
        );
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String text = stripper.getText(document).toUpperCase(Locale.ROOT);
            for (String needle : needles) {
                if (text.contains(needle.toUpperCase(Locale.ROOT))) {
                    return page - 1;
                }
            }
        }
        return -1;
    }

    private Crop broadQuestionCrop(BufferedImage image, Question question) {
        int marginX = Math.max(24, image.getWidth() / 25);
        int top = Math.max(24, image.getHeight() / 18);
        int height = (int) Math.round(image.getHeight() * 0.72);
        if (question.getSourceQuestionNumber() != null && question.getSourceQuestionNumber() > 1) {
            top = Math.max(24, image.getHeight() / 8);
            height = (int) Math.round(image.getHeight() * 0.68);
        }
        int width = image.getWidth() - (marginX * 2);
        height = Math.min(height, image.getHeight() - top - 24);
        return new Crop(marginX, top, width, height);
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
                .anyMatch(asset -> checksum.equalsIgnoreCase(String.valueOf(asset.getChecksum())));
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
                .replaceAll("(?i)<img[^>]+src=[\"']" + java.util.regex.Pattern.quote(BROKEN_IMAGE_URL) + "[\"'][^>]*>", "[Imagem oficial recuperada nos recursos da questão.]")
                .replace("![](" + BROKEN_IMAGE_URL + ")", "[Imagem oficial recuperada nos recursos da questão.]")
                .replace("![ ](" + BROKEN_IMAGE_URL + ")", "[Imagem oficial recuperada nos recursos da questão.]")
                .replace(BROKEN_IMAGE_URL, "");
    }

    private record Crop(int x, int y, int width, int height) {
    }
}
