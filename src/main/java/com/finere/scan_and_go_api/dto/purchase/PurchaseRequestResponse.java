package com.finere.scan_and_go_api.dto.purchase;

import com.finere.scan_and_go_api.domain.enums.PaymentMode;
import com.finere.scan_and_go_api.domain.enums.PurchaseRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestResponse(
        UUID id,
        UUID buyerOrgId,
        UUID sellerOrgId,
        UUID requestedByUserId,
        String requestedByName,
        PaymentMode paymentMode,
        UUID receivingWarehouseId,
        UUID receivingBoutiqueId,
        PurchaseRequestStatus status,
        String rejectionReason,
        UUID decidedByUserId,
        Instant decidedAt,
        UUID resultingOrderId,
        Instant createdAt,
        List<PurchaseRequestItemResponse> items
) {
}
