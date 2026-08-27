package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.product.ProductRequest;
import com.finere.scan_and_go_api.dto.product.ProductResponse;
import com.finere.scan_and_go_api.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/by-ean13/{ean13}")
    public ResponseEntity<ProductResponse> getByEan13(@PathVariable String ean13) {
        return ResponseEntity.ok(productService.getByEan13(ean13));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listByImporter(@RequestParam UUID importerId) {
        return ResponseEntity.ok(productService.listByImporter(importerId));
    }
}
