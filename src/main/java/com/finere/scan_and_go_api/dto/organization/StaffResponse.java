package com.finere.scan_and_go_api.dto.organization;

import com.finere.scan_and_go_api.domain.enums.UserRole;

import java.util.UUID;

public record StaffResponse(
        UUID id,
        UUID orgId,
        String phone,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        UUID assignedWarehouseId,
        UUID assignedBoutiqueId,
        boolean active
) {
}
