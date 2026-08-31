package com.finere.scan_and_go_api.dto.pos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RetailSaleResponse(
        UUID id,
        UUID boutiqueId,
        UUID soldByUserId,
        BigDecimal totalAmount,
        Instant createdAt,
        List<RetailSaleItemResponse> items
) {
}
