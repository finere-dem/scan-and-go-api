package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.organization.WarehouseRequest;
import com.finere.scan_and_go_api.dto.organization.WarehouseResponse;
import com.finere.scan_and_go_api.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'LOGISTICS_OPERATOR', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseResponse>> listByOrganization(@RequestParam UUID orgId) {
        return ResponseEntity.ok(warehouseService.listByOrganization(orgId));
    }
}
