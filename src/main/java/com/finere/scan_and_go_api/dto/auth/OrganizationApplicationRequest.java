package com.finere.scan_and_go_api.dto.auth;

import com.finere.scan_and_go_api.domain.enums.OrgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public self-service application: an importer/wholesaler/retailer signs itself
 * up rather than waiting for a super-admin to create it. The organization still
 * lands in PENDING_KYC (unchanged approval gate - a super admin must activate it
 * before it can be granted credit or trusted as a pricing counterparty), but the
 * business no longer has to ask someone to type its details in for it.
 */
public record OrganizationApplicationRequest(
        @NotBlank(message = "Organization name is required")
        String orgName,

        @NotBlank(message = "Tax ID (NIF/TIN) is required")
        String taxId,

        String rccm,

        @NotNull(message = "Organization type is required")
        OrgType orgType,

        String orgPhone,
        String orgEmail,
        String orgAddress,

        @NotBlank(message = "Admin phone is required")
        String adminPhone,

        String adminEmail,

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String adminPassword,

        String adminFirstName,
        String adminLastName
) {
}
