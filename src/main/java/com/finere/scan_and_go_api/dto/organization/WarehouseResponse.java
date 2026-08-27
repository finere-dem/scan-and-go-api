package com.finere.scan_and_go_api.dto.organization;

import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        UUID orgId,
        String name,
        String code,
        String address,
        Double latitude,
        Double longitude,
        boolean active
) {
}
