package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.product.ProductLotRequest;
import com.finere.scan_and_go_api.dto.product.ProductLotResponse;
import com.finere.scan_and_go_api.service.ProductLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-lots")
@RequiredArgsConstructor
public class ProductLotController {

    private final ProductLotService productLotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'LOGISTICS_OPERATOR', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN', 'BOUTIQUE_STAFF')")
    public ResponseEntity<ProductLotResponse> create(@Valid @RequestBody ProductLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productLotService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductLotResponse>> list(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID boutiqueId,
            @RequestParam(required = false) UUID productId) {
        if (warehouseId != null) {
            return ResponseEntity.ok(productLotService.listByWarehouse(warehouseId));
        }
        if (boutiqueId != null) {
            return ResponseEntity.ok(productLotService.listByBoutique(boutiqueId));
        }
        if (productId != null) {
            return ResponseEntity.ok(productLotService.listByProduct(productId));
        }
        throw new IllegalArgumentException("Either warehouseId, boutiqueId, or productId must be provided");
    }
}
