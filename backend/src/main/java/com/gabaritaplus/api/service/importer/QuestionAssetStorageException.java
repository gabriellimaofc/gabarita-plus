package com.gabaritaplus.api.service.importer;

public class QuestionAssetStorageException extends RuntimeException {

    private final String reason;

    public QuestionAssetStorageException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public QuestionAssetStorageException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
