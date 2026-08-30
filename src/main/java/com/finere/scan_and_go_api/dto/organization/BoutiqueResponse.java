package com.finere.scan_and_go_api.dto.organization;

import java.util.UUID;

public record BoutiqueResponse(
        UUID id,
        UUID orgId,
        String name,
        String code,
        String address,
        boolean active
) {
}
