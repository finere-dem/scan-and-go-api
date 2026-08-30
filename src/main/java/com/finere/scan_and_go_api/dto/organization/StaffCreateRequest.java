package com.finere.scan_and_go_api.dto.organization;

import com.finere.scan_and_go_api.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Lets an organization create its own staff, scoped to one depot or shop -
 * unlike /api/auth/register (open self-registration), the org is always the
 * path parameter here, never client-supplied, and the role is restricted to
 * the two staff-level roles so an org admin can never mint another admin.
 */
public record StaffCreateRequest(
        @NotBlank(message = "Phone is required")
        String phone,

        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String firstName,
        String lastName,

        @NotNull(message = "Role is required")
        UserRole role,

        UUID assignedWarehouseId,
        UUID assignedBoutiqueId
) {
}
