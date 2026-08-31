package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.domain.enums.PurchaseRequestStatus;
import com.finere.scan_and_go_api.dto.order.OrderResponse;
import com.finere.scan_and_go_api.dto.purchase.PurchaseRequestCreateRequest;
import com.finere.scan_and_go_api.dto.purchase.PurchaseRequestRejectRequest;
import com.finere.scan_and_go_api.dto.purchase.PurchaseRequestResponse;
import com.finere.scan_and_go_api.service.PurchaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PURCHASE_STAFF')")
    public ResponseEntity<PurchaseRequestResponse> create(@Valid @RequestBody PurchaseRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseRequestService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN', 'PURCHASE_STAFF')")
    public ResponseEntity<List<PurchaseRequestResponse>> listByBuyer(
            @RequestParam UUID buyerOrgId, @RequestParam(required = false) PurchaseRequestStatus status) {
        return ResponseEntity.ok(purchaseRequestService.listByBuyer(buyerOrgId, status));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<OrderResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseRequestService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<PurchaseRequestResponse> reject(
            @PathVariable UUID id, @Valid @RequestBody PurchaseRequestRejectRequest request) {
        return ResponseEntity.ok(purchaseRequestService.reject(id, request));
    }
}
