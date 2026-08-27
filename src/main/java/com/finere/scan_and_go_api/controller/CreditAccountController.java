package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.dto.audit.CreditAccountRevisionResponse;
import com.finere.scan_and_go_api.dto.pricing.CreditAccountRequest;
import com.finere.scan_and_go_api.dto.pricing.CreditAccountResponse;
import com.finere.scan_and_go_api.service.CreditAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credit-accounts")
@RequiredArgsConstructor
public class CreditAccountController {

    private final CreditAccountService creditAccountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IMPORTER_ADMIN', 'WHOLESALER_ADMIN')")
    public ResponseEntity<CreditAccountResponse> create(@Valid @RequestBody CreditAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditAccountService.create(request));
    }

    @GetMapping(params = {"creditorOrgId", "debtorOrgId"})
    public ResponseEntity<CreditAccountResponse> getByCreditorAndDebtor(
            @RequestParam UUID creditorOrgId, @RequestParam UUID debtorOrgId) {
        return ResponseEntity.ok(creditAccountService.getByCreditorAndDebtor(creditorOrgId, debtorOrgId));
    }

    @GetMapping(params = "creditorOrgId")
    public ResponseEntity<List<CreditAccountResponse>> listByCreditor(@RequestParam UUID creditorOrgId) {
        return ResponseEntity.ok(creditAccountService.listByCreditor(creditorOrgId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<CreditAccountRevisionResponse>> listRevisions(@PathVariable UUID id) {
        return ResponseEntity.ok(creditAccountService.listRevisions(id));
    }
}
