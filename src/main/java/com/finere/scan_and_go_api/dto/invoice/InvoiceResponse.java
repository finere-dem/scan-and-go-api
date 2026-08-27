package com.finere.scan_and_go_api.dto.invoice;

import com.finere.scan_and_go_api.domain.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID buyerOrgId,
        UUID sellerOrgId,
        String invoiceNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        InvoiceStatus status
) {
}
