package com.finere.scan_and_go_api.dto.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LocalRetailPriceResponse(
        UUID id,
        UUID retailerOrgId,
        UUID productId,
        BigDecimal consumerPrice,
        String currency,
        Instant updatedAt
) {
}
