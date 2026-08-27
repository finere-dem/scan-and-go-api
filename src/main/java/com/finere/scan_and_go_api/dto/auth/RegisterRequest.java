package com.finere.scan_and_go_api.dto.auth;

import com.finere.scan_and_go_api.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(

        @NotBlank(message = "Phone is required")
        String phone,

        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String firstName,
        String lastName,

        @NotNull(message = "Role is required")
        UserRole role,

        UUID orgId
) {
}
