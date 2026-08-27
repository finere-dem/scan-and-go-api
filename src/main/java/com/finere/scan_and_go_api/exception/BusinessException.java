package com.finere.scan_and_go_api.exception;

import org.springframework.http.HttpStatus;

/** Base type for domain-rule violations that should surface as RFC 7807 ProblemDetail responses. */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
