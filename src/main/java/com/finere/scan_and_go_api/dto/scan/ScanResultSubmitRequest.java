package com.finere.scan_and_go_api.dto.scan;

import jakarta.validation.constraints.NotBlank;

public record ScanResultSubmitRequest(
        @NotBlank(message = "Scanned code is required")
        String code
) {
}
