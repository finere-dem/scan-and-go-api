package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.organization.StaffCreateRequest;
import com.finere.scan_and_go_api.dto.organization.StaffResponse;
import com.finere.scan_and_go_api.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{orgId}/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<StaffResponse> create(@PathVariable UUID orgId, @Valid @RequestBody StaffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(orgId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<List<StaffResponse>> listByOrganization(@PathVariable UUID orgId) {
        return ResponseEntity.ok(staffService.listByOrganization(orgId));
    }
}
