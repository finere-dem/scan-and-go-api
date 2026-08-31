package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.pricing.PricingPolicyRequest;
import com.finere.scan_and_go_api.dto.pricing.PricingPolicyResponse;
import com.finere.scan_and_go_api.service.PricingPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pricing-policies")
@RequiredArgsConstructor
public class PricingPolicyController {

    private final PricingPolicyService pricingPolicyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'WHOLESALER_ADMIN', 'SALES_STAFF')")
    public ResponseEntity<PricingPolicyResponse> create(@Valid @RequestBody PricingPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingPolicyService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PricingPolicyResponse>> listBySeller(@RequestParam UUID sellerOrgId) {
        return ResponseEntity.ok(pricingPolicyService.listBySeller(sellerOrgId));
    }
}
