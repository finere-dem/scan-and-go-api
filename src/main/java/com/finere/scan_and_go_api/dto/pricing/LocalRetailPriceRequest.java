package com.finere.scan_and_go_api.dto.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record LocalRetailPriceRequest(
        @NotNull(message = "Retailer organization is required")
        UUID retailerOrgId,

        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Consumer price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Consumer price must be positive")
        BigDecimal consumerPrice,

        String currency
) {
}
