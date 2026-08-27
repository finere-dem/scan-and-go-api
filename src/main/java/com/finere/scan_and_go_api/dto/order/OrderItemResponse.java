package com.finere.scan_and_go_api.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        UUID lotId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
