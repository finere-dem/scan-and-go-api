package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.invoice.InvoiceResponse;
import com.finere.scan_and_go_api.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping(params = "buyerOrgId")
    public ResponseEntity<List<InvoiceResponse>> listByBuyer(@RequestParam UUID buyerOrgId) {
        return ResponseEntity.ok(invoiceService.listByBuyer(buyerOrgId));
    }

    @GetMapping(params = "sellerOrgId")
    public ResponseEntity<List<InvoiceResponse>> listBySeller(@RequestParam UUID sellerOrgId) {
        return ResponseEntity.ok(invoiceService.listBySeller(sellerOrgId));
    }
}
