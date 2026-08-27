package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.organization.OrganizationRequest;
import com.finere.scan_and_go_api.dto.organization.OrganizationResponse;
import com.finere.scan_and_go_api.dto.organization.OrganizationStatusUpdateRequest;
import com.finere.scan_and_go_api.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizationResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody OrganizationStatusUpdateRequest request) {
        return ResponseEntity.ok(organizationService.updateStatus(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> listAll() {
        return ResponseEntity.ok(organizationService.listAll());
    }
}
