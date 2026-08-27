package com.finere.scan_and_go_api.dto.pricing;

import com.finere.scan_and_go_api.domain.enums.CreditStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditAccountResponse(
        UUID id,
        UUID creditorOrgId,
        UUID debtorOrgId,
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        int paymentTermDays,
        CreditStatus status
) {
}
