package com.finere.scan_and_go_api.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Enforces organization-scoped access: a role check alone (e.g. hasRole('IMPORTER_ADMIN')) only
 * proves what kind of user is calling, not whose data they're touching - without this, any
 * IMPORTER_ADMIN token could create resources under a different org's id just by changing the
 * request body. SUPER_ADMIN is exempt since it operates across all organizations by design.
 */
@Service
public class CurrentUserService {

    public UUID requireOrgId() {
        return currentOrgId()
                .orElseThrow(() -> new AccessDeniedException("Current user is not attached to an organization"));
    }

    public boolean isSuperAdmin() {
        return currentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")))
                .orElse(false);
    }

    /** No-op for SUPER_ADMIN; otherwise requires the caller's own org to match {@code targetOrgId}. */
    public void requireSameOrgOrSuperAdmin(UUID targetOrgId) {
        if (isSuperAdmin()) {
            return;
        }
        UUID callerOrgId = requireOrgId();
        if (!callerOrgId.equals(targetOrgId)) {
            throw new AccessDeniedException(
                    "Organization " + callerOrgId + " cannot act on behalf of organization " + targetOrgId);
        }
    }

    private Optional<UUID> currentOrgId() {
        return currentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(CustomUserDetails.class::isInstance)
                .map(CustomUserDetails.class::cast)
                .map(CustomUserDetails::getOrgId);
    }

    private Optional<Authentication> currentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }
}
