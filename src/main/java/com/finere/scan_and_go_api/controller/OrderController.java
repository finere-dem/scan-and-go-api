package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.order.OrderCreateRequest;
import com.finere.scan_and_go_api.dto.order.OrderResponse;
import com.finere.scan_and_go_api.service.OrderService;
import com.finere.scan_and_go_api.service.PriceLabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PriceLabelService priceLabelService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'WHOLESALER_ADMIN', 'RETAILER_ADMIN')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping(params = "buyerOrgId")
    public ResponseEntity<List<OrderResponse>> listByBuyer(@RequestParam UUID buyerOrgId) {
        return ResponseEntity.ok(orderService.listByBuyer(buyerOrgId));
    }

    @GetMapping(params = "sellerOrgId")
    public ResponseEntity<List<OrderResponse>> listBySeller(@RequestParam UUID sellerOrgId) {
        return ResponseEntity.ok(orderService.listBySeller(sellerOrgId));
    }

    /** One printable price label per unit ordered - the seller sticks these on before delivery,
     * with the buyer's own downstream sale price already printed on (see PriceLabelService). */
    @GetMapping(value = "/{orderId}/price-labels.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> priceLabelsPdf(@PathVariable UUID orderId) {
        return ResponseEntity.ok(priceLabelService.generateForOrder(orderId));
    }
}
