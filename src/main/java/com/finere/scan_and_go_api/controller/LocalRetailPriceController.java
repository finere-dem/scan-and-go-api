package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.pricing.LocalRetailPriceRequest;
import com.finere.scan_and_go_api.dto.pricing.LocalRetailPriceResponse;
import com.finere.scan_and_go_api.service.LocalRetailPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/local-retail-prices")
@RequiredArgsConstructor
public class LocalRetailPriceController {

    private final LocalRetailPriceService localRetailPriceService;

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RETAILER_ADMIN', 'SALES_STAFF')")
    public ResponseEntity<LocalRetailPriceResponse> upsert(@Valid @RequestBody LocalRetailPriceRequest request) {
        return ResponseEntity.ok(localRetailPriceService.upsert(request));
    }

    @GetMapping(params = {"retailerOrgId", "productId"})
    public ResponseEntity<LocalRetailPriceResponse> getByRetailerAndProduct(
            @RequestParam UUID retailerOrgId, @RequestParam UUID productId) {
        return ResponseEntity.ok(localRetailPriceService.getByRetailerAndProduct(retailerOrgId, productId));
    }

    @GetMapping(params = "retailerOrgId")
    public ResponseEntity<List<LocalRetailPriceResponse>> listByRetailer(@RequestParam UUID retailerOrgId) {
        return ResponseEntity.ok(localRetailPriceService.listByRetailer(retailerOrgId));
    }
}
