package com.finere.scan_and_go_api.dto.organization;

import com.finere.scan_and_go_api.domain.enums.OrgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrganizationRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Tax ID (NIF/TIN) is required")
        String taxId,

        String rccm,

        @NotNull(message = "Organization type is required")
        OrgType orgType,

        String phone,
        String email,
        String address
) {
}
