package com.finere.scan_and_go_api.dto.order;

import com.finere.scan_and_go_api.domain.enums.OrderStatus;
import com.finere.scan_and_go_api.domain.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID buyerOrgId,
        UUID sellerOrgId,
        BigDecimal totalAmount,
        PaymentMode paymentMode,
        OrderStatus orderStatus,
        UUID clientSyncId,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
