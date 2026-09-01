package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.scan.ScanResultSubmitRequest;
import com.finere.scan_and_go_api.dto.scan.ScanSessionResponse;
import com.finere.scan_and_go_api.service.ScanSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/scan-sessions")
@RequiredArgsConstructor
public class ScanSessionController {

    private final ScanSessionService scanSessionService;

    /** Desktop side: start a pairing session. Requires auth (any authenticated role). */
    @PostMapping
    public ResponseEntity<ScanSessionResponse> create() {
        return ResponseEntity.status(HttpStatus.CREATED).body(scanSessionService.create());
    }

    /** Desktop side: poll for the result. Requires auth. */
    @GetMapping("/{id}")
    public ResponseEntity<ScanSessionResponse> getStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(scanSessionService.getStatus(id));
    }

    /** Phone side: submit the scanned code. Public - see SecurityConfig and ScanSessionService docs. */
    @PostMapping("/{id}/result")
    public ResponseEntity<Void> submitResult(@PathVariable UUID id, @Valid @RequestBody ScanResultSubmitRequest request) {
        scanSessionService.submitResult(id, request);
        return ResponseEntity.noContent().build();
    }
}
