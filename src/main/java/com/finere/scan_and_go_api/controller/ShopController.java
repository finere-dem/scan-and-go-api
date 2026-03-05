package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.CheckoutRequest;
import com.finere.scan_and_go_api.dto.ScanResponse;
import com.finere.scan_and_go_api.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.finere.scan_and_go_api.model.Store;
import com.finere.scan_and_go_api.repository.StoreRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all origins for mobile access
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private StoreRepository storeRepository;

    @GetMapping("/scan")
    public ResponseEntity<ScanResponse> scanProduct(@RequestParam("barcode") String barcode,
            @RequestParam("storeId") Long storeId) {
        try {
            return ResponseEntity.ok(shopService.scanProduct(barcode, storeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<Long> checkout(@RequestBody CheckoutRequest request) {
        try {
            Long orderId = shopService.checkout(request);
            return ResponseEntity.ok(orderId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stores")
    public ResponseEntity<List<Store>> getAllStores() {
        return ResponseEntity.ok(storeRepository.findAll());
    }
}
