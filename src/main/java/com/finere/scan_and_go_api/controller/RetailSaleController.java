package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.pos.RetailSaleCreateRequest;
import com.finere.scan_and_go_api.dto.pos.RetailSaleResponse;
import com.finere.scan_and_go_api.service.RetailSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/retail-sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RETAILER_ADMIN', 'BOUTIQUE_STAFF')")
public class RetailSaleController {

    private final RetailSaleService retailSaleService;

    @PostMapping
    public ResponseEntity<RetailSaleResponse> create(@Valid @RequestBody RetailSaleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(retailSaleService.createSale(request));
    }

    @GetMapping
    public ResponseEntity<List<RetailSaleResponse>> listByBoutique(@RequestParam UUID boutiqueId) {
        return ResponseEntity.ok(retailSaleService.listByBoutique(boutiqueId));
    }
}
