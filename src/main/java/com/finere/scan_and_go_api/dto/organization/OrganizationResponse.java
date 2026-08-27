package com.finere.scan_and_go_api.dto.organization;

import com.finere.scan_and_go_api.domain.enums.OrgStatus;
import com.finere.scan_and_go_api.domain.enums.OrgType;

import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String taxId,
        String rccm,
        OrgType orgType,
        OrgStatus status,
        String phone,
        String email,
        String address
) {
}
