package com.finere.scan_and_go_api.dto.product;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID importerId,
        String sku,
        String ean13,
        String name,
        String brand,
        String category,
        String packagingType,
        Integer unitsPerBox
) {
}
