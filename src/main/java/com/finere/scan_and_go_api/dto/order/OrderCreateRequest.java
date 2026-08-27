package com.finere.scan_and_go_api.dto.order;

import com.finere.scan_and_go_api.domain.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull(message = "Buyer organization is required")
        UUID buyerOrgId,

        @NotNull(message = "Seller organization is required")
        UUID sellerOrgId,

        @NotNull(message = "Payment mode is required")
        PaymentMode paymentMode,

        /** Client-generated UUID for offline-first idempotent sync; a repeat submission returns the original order. */
        UUID clientSyncId,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {
}
