package com.finere.scan_and_go_api.dto.audit;

import com.finere.scan_and_go_api.domain.enums.CreditStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** One historical snapshot of a CreditAccount, as recorded by Hibernate Envers. */
public record CreditAccountRevisionResponse(
        int revisionNumber,
        Instant revisionTimestamp,
        String revisionType,
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        CreditStatus status
) {
}
