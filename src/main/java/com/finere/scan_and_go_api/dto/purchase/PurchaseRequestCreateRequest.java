package com.finere.scan_and_go_api.dto.purchase;

import com.finere.scan_and_go_api.domain.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PurchaseRequestCreateRequest(
        @NotNull(message = "Buyer organization is required")
        UUID buyerOrgId,

        @NotNull(message = "Seller organization is required")
        UUID sellerOrgId,

        @NotNull(message = "Payment mode is required")
        PaymentMode paymentMode,

        UUID receivingWarehouseId,
        UUID receivingBoutiqueId,

        @NotEmpty(message = "Purchase request must contain at least one item")
        @Valid
        List<PurchaseRequestItemInput> items
) {
}
