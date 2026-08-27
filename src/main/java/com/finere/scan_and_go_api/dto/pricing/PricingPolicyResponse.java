package com.finere.scan_and_go_api.dto.pricing;

import com.finere.scan_and_go_api.domain.enums.TargetOrgType;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingPolicyResponse(
        UUID id,
        UUID sellerOrgId,
        UUID productId,
        TargetOrgType targetOrgType,
        BigDecimal unitPrice,
        int minOrderQty,
        String currency
) {
}
