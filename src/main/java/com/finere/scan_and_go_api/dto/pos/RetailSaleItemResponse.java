package com.finere.scan_and_go_api.dto.pos;

import java.math.BigDecimal;
import java.util.UUID;

public record RetailSaleItemResponse(
        UUID productId,
        UUID lotId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
