package com.finere.scan_and_go_api.dto.scan;

import com.finere.scan_and_go_api.domain.enums.ScanSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record ScanSessionResponse(
        UUID id,
        ScanSessionStatus status,
        String scannedCode,
        Instant expiresAt
) {
}
