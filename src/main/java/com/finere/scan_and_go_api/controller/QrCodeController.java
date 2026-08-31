package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.qr.LabelSheetBatchRequest;
import com.finere.scan_and_go_api.dto.qr.QrCodeResult;
import com.finere.scan_and_go_api.service.QrCodeGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/qr-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'LOGISTICS_OPERATOR')")
public class QrCodeController {

    private final QrCodeGenerationService qrCodeGenerationService;

    @GetMapping(value = "/products/{productId}/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> productQrPng(@PathVariable UUID productId) {
        QrCodeResult result = qrCodeGenerationService.generateForProduct(productId);
        return ResponseEntity.ok(result.pngBytes());
    }

    @GetMapping(value = "/lots/{lotId}/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> lotQrPng(@PathVariable UUID lotId) {
        QrCodeResult result = qrCodeGenerationService.generateForLot(lotId);
        return ResponseEntity.ok(result.pngBytes());
    }

    @GetMapping(value = "/products/{productId}/shelf-poster/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> shelfPosterPng(@PathVariable UUID productId) {
        QrCodeResult result = qrCodeGenerationService.generateShelfPoster(productId);
        return ResponseEntity.ok(result.pngBytes());
    }

    @GetMapping(value = "/products/{productId}/label-sheet.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> labelSheetPdf(@PathVariable UUID productId) {
        byte[] pdf = qrCodeGenerationService.buildLabelSheetForProduct(productId);
        return ResponseEntity.ok(pdf);
    }

    /** One combined printable sheet across several selected products - the catalog export a
     * retailer/wholesaler actually wants, rather than one PDF per product downloaded one at a time. */
    @PostMapping(value = "/label-sheet.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> labelSheetPdfBatch(@Valid @RequestBody LabelSheetBatchRequest request) {
        byte[] pdf = qrCodeGenerationService.buildLabelSheetForProducts(request.productIds());
        return ResponseEntity.ok(pdf);
    }
}
