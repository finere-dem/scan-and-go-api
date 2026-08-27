package com.finere.scan_and_go_api.dto.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditAccountRequest(
        @NotNull(message = "Creditor organization is required")
        UUID creditorOrgId,

        @NotNull(message = "Debtor organization is required")
        UUID debtorOrgId,

        @NotNull(message = "Credit limit is required")
        @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
        BigDecimal creditLimit,

        /** Must be one of 0, 30, 60, 90 per the payment_term_days check constraint. */
        int paymentTermDays
) {
}
