package com.finere.scan_and_go_api.dto.purchase;

import jakarta.validation.constraints.NotBlank;

public record PurchaseRequestRejectRequest(
        @NotBlank(message = "A rejection reason is required")
        String reason
) {
}
