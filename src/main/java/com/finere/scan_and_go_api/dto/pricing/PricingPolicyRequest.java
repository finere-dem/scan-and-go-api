package com.finere.scan_and_go_api.dto.pricing;

import com.finere.scan_and_go_api.domain.enums.TargetOrgType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingPolicyRequest(
        @NotNull(message = "Seller organization is required")
        UUID sellerOrgId,

        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Target organization type is required")
        TargetOrgType targetOrgType,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be positive")
        BigDecimal unitPrice,

        @Min(value = 1, message = "Minimum order quantity must be at least 1")
        int minOrderQty,

        String currency
) {
}
