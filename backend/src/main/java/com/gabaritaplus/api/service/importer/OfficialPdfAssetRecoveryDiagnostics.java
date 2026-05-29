package com.gabaritaplus.api.service.importer;

import java.util.ArrayList;
import java.util.List;

public record OfficialPdfAssetRecoveryDiagnostics(
        boolean recoveryAttempted,
        boolean officialSourceFound,
        boolean pdfDownloaded,
        Long pdfSizeBytes,
        Integer pdfDownloadHttpStatus,
        String pdfDownloadContentType,
        Long pdfDownloadContentLength,
        String pdfDownloadErrorMessage,
        String pdfUrlUsed,
        Integer pdfPageCount,
        List<Integer> candidatePages,
        Integer selectedPage,
        boolean pdfRendered,
        Integer renderedWidth,
        Integer renderedHeight,
        boolean storageUploadAttempted,
        boolean storageUploadSuccess,
        String recoveryFailureReason,
        String recoveryMethod,
        String assetUrl
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean recoveryAttempted;
        private boolean officialSourceFound;
        private boolean pdfDownloaded;
        private Long pdfSizeBytes;
        private Integer pdfDownloadHttpStatus;
        private String pdfDownloadContentType;
        private Long pdfDownloadContentLength;
        private String pdfDownloadErrorMessage;
        private String pdfUrlUsed;
        private Integer pdfPageCount;
        private List<Integer> candidatePages = new ArrayList<>();
        private Integer selectedPage;
        private boolean pdfRendered;
        private Integer renderedWidth;
        private Integer renderedHeight;
        private boolean storageUploadAttempted;
        private boolean storageUploadSuccess;
        private String recoveryFailureReason;
        private String recoveryMethod;
        private String assetUrl;

        public Builder recoveryAttempted(boolean value) {
            this.recoveryAttempted = value;
            return this;
        }

        public Builder officialSourceFound(boolean value) {
            this.officialSourceFound = value;
            return this;
        }

        public Builder pdfDownloaded(boolean value) {
            this.pdfDownloaded = value;
            return this;
        }

        public Builder pdfSizeBytes(Long value) {
            this.pdfSizeBytes = value;
            return this;
        }

        public Builder pdfDownloadHttpStatus(Integer value) {
            this.pdfDownloadHttpStatus = value;
            return this;
        }

        public Builder pdfDownloadContentType(String value) {
            this.pdfDownloadContentType = value;
            return this;
        }

        public Builder pdfDownloadContentLength(Long value) {
            this.pdfDownloadContentLength = value;
            return this;
        }

        public Builder pdfDownloadErrorMessage(String value) {
            this.pdfDownloadErrorMessage = value;
            return this;
        }

        public Builder pdfUrlUsed(String value) {
            this.pdfUrlUsed = value;
            return this;
        }

        public Builder pdfPageCount(Integer value) {
            this.pdfPageCount = value;
            return this;
        }

        public Builder candidatePages(List<Integer> value) {
            this.candidatePages = value == null ? new ArrayList<>() : new ArrayList<>(value);
            return this;
        }

        public Builder selectedPage(Integer value) {
            this.selectedPage = value;
            return this;
        }

        public Builder pdfRendered(boolean value) {
            this.pdfRendered = value;
            return this;
        }

        public Builder renderedWidth(Integer value) {
            this.renderedWidth = value;
            return this;
        }

        public Builder renderedHeight(Integer value) {
            this.renderedHeight = value;
            return this;
        }

        public Builder storageUploadAttempted(boolean value) {
            this.storageUploadAttempted = value;
            return this;
        }

        public Builder storageUploadSuccess(boolean value) {
            this.storageUploadSuccess = value;
            return this;
        }

        public Builder recoveryFailureReason(String value) {
            this.recoveryFailureReason = value;
            return this;
        }

        public Builder recoveryMethod(String value) {
            this.recoveryMethod = value;
            return this;
        }

        public Builder assetUrl(String value) {
            this.assetUrl = value;
            return this;
        }

        public OfficialPdfAssetRecoveryDiagnostics build() {
            return new OfficialPdfAssetRecoveryDiagnostics(
                    recoveryAttempted,
                    officialSourceFound,
                    pdfDownloaded,
                    pdfSizeBytes,
                    pdfDownloadHttpStatus,
                    pdfDownloadContentType,
                    pdfDownloadContentLength,
                    pdfDownloadErrorMessage,
                    pdfUrlUsed,
                    pdfPageCount,
                    List.copyOf(candidatePages),
                    selectedPage,
                    pdfRendered,
                    renderedWidth,
                    renderedHeight,
                    storageUploadAttempted,
                    storageUploadSuccess,
                    recoveryFailureReason,
                    recoveryMethod,
                    assetUrl
            );
        }
    }
}
