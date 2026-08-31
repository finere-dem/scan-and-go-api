package com.finere.scan_and_go_api.dto.purchase;

import java.util.UUID;

public record PurchaseRequestItemResponse(
        UUID productId,
        UUID warehouseId,
        int quantity
) {
}
