package com.finere.scan_and_go_api.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Warehouse is required")
        UUID warehouseId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}
