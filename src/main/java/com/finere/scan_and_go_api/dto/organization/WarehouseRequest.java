package com.finere.scan_and_go_api.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WarehouseRequest(
        @NotNull(message = "Organization is required")
        UUID orgId,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Code is required")
        String code,

        String address,
        Double latitude,
        Double longitude
) {
}
