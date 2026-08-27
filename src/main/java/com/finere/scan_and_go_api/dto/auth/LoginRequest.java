package com.finere.scan_and_go_api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Password is required")
        String password
) {
}
