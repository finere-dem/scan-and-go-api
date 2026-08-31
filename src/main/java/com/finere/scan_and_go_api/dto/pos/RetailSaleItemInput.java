package com.finere.scan_and_go_api.dto.pos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RetailSaleItemInput(
        @NotNull(message = "Product is required")
        UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}
