package com.gabaritaplus.api.exception;

import org.springframework.http.HttpStatus;

public class ExternalRateLimitException extends BusinessException {

    public ExternalRateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
