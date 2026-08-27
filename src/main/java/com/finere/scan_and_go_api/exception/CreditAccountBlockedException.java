package com.finere.scan_and_go_api.exception;

import org.springframework.http.HttpStatus;

public class CreditAccountBlockedException extends BusinessException {
    public CreditAccountBlockedException(String message) {
        super(HttpStatus.FORBIDDEN, "CREDIT_ACCOUNT_LOCKED", message);
    }
}
