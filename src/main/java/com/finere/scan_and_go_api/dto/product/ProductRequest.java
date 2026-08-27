package com.finere.scan_and_go_api.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductRequest(
        @NotNull(message = "Importer organization is required")
        UUID importerId,

        @NotBlank(message = "SKU is required")
        String sku,

        @Size(min = 13, max = 13, message = "EAN-13 must be exactly 13 characters")
        String ean13,

        @NotBlank(message = "Name is required")
        String name,

        String brand,
        String category,
        String packagingType,
        Integer unitsPerBox
) {
}
