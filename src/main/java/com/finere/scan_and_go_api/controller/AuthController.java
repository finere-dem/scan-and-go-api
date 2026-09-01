package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.auth.AuthResponse;
import com.finere.scan_and_go_api.dto.auth.LoginRequest;
import com.finere.scan_and_go_api.dto.auth.OrganizationApplicationRequest;
import com.finere.scan_and_go_api.dto.auth.RefreshRequest;
import com.finere.scan_and_go_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/apply-organization")
    public ResponseEntity<AuthResponse> applyOrganization(@Valid @RequestBody OrganizationApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.applyOrganization(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
