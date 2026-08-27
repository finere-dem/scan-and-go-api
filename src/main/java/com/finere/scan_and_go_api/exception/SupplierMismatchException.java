package com.finere.scan_and_go_api.exception;

import org.springframework.http.HttpStatus;

/** Raised when a cart scan attempts to mix products from two different seller organizations. */
public class SupplierMismatchException extends BusinessException {
    public SupplierMismatchException(String message) {
        super(HttpStatus.CONFLICT, "SUPPLIER_MISMATCH", message);
    }
}
