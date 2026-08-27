package com.finere.scan_and_go_api.controller;

import com.finere.scan_and_go_api.service.CreditManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual triggers for jobs that normally run on a schedule - useful for ops and support. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final CreditManagementService creditManagementService;

    @PostMapping("/credit/run-overdue-sweep")
    public ResponseEntity<Void> runOverdueSweep() {
        creditManagementService.runNightlyOverdueSweep();
        return ResponseEntity.ok().build();
    }
}
