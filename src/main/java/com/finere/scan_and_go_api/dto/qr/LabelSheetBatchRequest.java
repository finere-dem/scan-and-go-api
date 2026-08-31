package com.finere.scan_and_go_api.dto.qr;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record LabelSheetBatchRequest(
        @NotEmpty(message = "At least one product must be selected")
        List<UUID> productIds
) {
}
