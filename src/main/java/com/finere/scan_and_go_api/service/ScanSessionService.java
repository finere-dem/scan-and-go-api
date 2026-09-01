package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.ScanSession;
import com.finere.scan_and_go_api.domain.enums.ScanSessionStatus;
import com.finere.scan_and_go_api.dto.scan.ScanResultSubmitRequest;
import com.finere.scan_and_go_api.dto.scan.ScanSessionResponse;
import com.finere.scan_and_go_api.repository.ScanSessionRepository;
import com.finere.scan_and_go_api.repository.UserRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Pairs a desktop screen with a phone's browser camera: the desktop creates a session and polls
 * it, the phone (unauthenticated - it never logs in) submits the scanned code against the same
 * session id. The id itself (a random UUID) is the only credential the phone side ever needs,
 * which is why {@link #submitResult} must stay public in SecurityConfig - anyone who doesn't
 * already know a live session id has nothing to guess.
 */
@Service
@RequiredArgsConstructor
public class ScanSessionService {

    private static final int SESSION_TTL_MINUTES = 5;

    private final ScanSessionRepository scanSessionRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public ScanSessionResponse create() {
        ScanSession session = new ScanSession();
        session.setCreatedBy(userRepository.getReferenceById(currentUserService.requireUserId()));
        session.setExpiresAt(Instant.now().plus(SESSION_TTL_MINUTES, ChronoUnit.MINUTES));
        return toResponse(scanSessionRepository.save(session));
    }

    @Transactional
    public ScanSessionResponse getStatus(UUID id) {
        ScanSession session = find(id);
        expireIfPastDeadline(session);
        return toResponse(session);
    }

    /** Public endpoint - the phone side never authenticates, see class docs. */
    @Transactional
    public void submitResult(UUID id, ScanResultSubmitRequest request) {
        ScanSession session = find(id);
        expireIfPastDeadline(session);
        if (session.getStatus() != ScanSessionStatus.PENDING) {
            throw new IllegalStateException("Scan session " + id + " is no longer accepting a result (" + session.getStatus() + ")");
        }
        session.setScannedCode(request.code());
        session.setStatus(ScanSessionStatus.CONSUMED);
        scanSessionRepository.save(session);
    }

    private ScanSession find(UUID id) {
        return scanSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown scan session: " + id));
    }

    private void expireIfPastDeadline(ScanSession session) {
        if (session.getStatus() == ScanSessionStatus.PENDING && Instant.now().isAfter(session.getExpiresAt())) {
            session.setStatus(ScanSessionStatus.EXPIRED);
            scanSessionRepository.save(session);
        }
    }

    private ScanSessionResponse toResponse(ScanSession session) {
        return new ScanSessionResponse(session.getId(), session.getStatus(), session.getScannedCode(), session.getExpiresAt());
    }
}
