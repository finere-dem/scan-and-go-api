package com.finere.scan_and_go_api.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", message);
    }
}
