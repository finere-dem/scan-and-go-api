package com.finere.scan_and_go_api.exception;

import org.springframework.http.HttpStatus;

public class CreditLimitExceededException extends BusinessException {
    public CreditLimitExceededException(String message) {
        super(HttpStatus.CONFLICT, "CREDIT_LIMIT_EXCEEDED", message);
    }
}
