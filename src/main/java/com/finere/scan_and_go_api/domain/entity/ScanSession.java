package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.ScanSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Pairs a desktop screen (creates the session, polls for the result) with a phone's
 * browser camera (reads the code, posts it back) - see ScanSessionService. */
@Entity
@Table(name = "scan_sessions")
@Getter
@Setter
public class ScanSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanSessionStatus status = ScanSessionStatus.PENDING;

    @Column(name = "scanned_code")
    private String scannedCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
