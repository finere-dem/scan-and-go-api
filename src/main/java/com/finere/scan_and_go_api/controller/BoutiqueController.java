package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.organization.BoutiqueRequest;
import com.finere.scan_and_go_api.dto.organization.BoutiqueResponse;
import com.finere.scan_and_go_api.service.BoutiqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/boutiques")
@RequiredArgsConstructor
public class BoutiqueController {

    private final BoutiqueService boutiqueService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<BoutiqueResponse> create(@Valid @RequestBody BoutiqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boutiqueService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BoutiqueResponse>> listByOrganization(@RequestParam UUID orgId) {
        return ResponseEntity.ok(boutiqueService.listByOrganization(orgId));
    }
}
