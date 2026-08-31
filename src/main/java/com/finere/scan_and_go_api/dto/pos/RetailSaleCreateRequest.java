package com.finere.scan_and_go_api.dto.pos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RetailSaleCreateRequest(
        @NotNull(message = "Boutique is required")
        UUID boutiqueId,

        @NotEmpty(message = "Sale must contain at least one item")
        @Valid
        List<RetailSaleItemInput> items
) {
}
