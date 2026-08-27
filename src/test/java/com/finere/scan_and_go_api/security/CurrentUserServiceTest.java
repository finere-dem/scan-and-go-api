package com.finere.scan_and_go_api.security;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.User;
import com.finere.scan_and_go_api.domain.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The org-scoping guard sits in front of every mutating endpoint in the app (warehouses,
 * products, pricing, credit, orders, QR generation) - if this is wrong, a request that should
 * be rejected silently succeeds against the wrong organization's data.
 */
class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID orgId, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPhone("+22300000000");
        user.setRole(role);
        if (orgId != null) {
            Organization org = new Organization();
            org.setId(orgId);
            user.setOrganization(org);
        }

        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void requireSameOrgOrSuperAdminPassesWhenCallerOwnsTheTargetOrg() {
        UUID orgId = UUID.randomUUID();
        authenticateAs(orgId, UserRole.ROLE_IMPORTER_ADMIN);

        service.requireSameOrgOrSuperAdmin(orgId);
    }

    @Test
    void requireSameOrgOrSuperAdminRejectsADifferentOrg() {
        authenticateAs(UUID.randomUUID(), UserRole.ROLE_IMPORTER_ADMIN);

        assertThatThrownBy(() -> service.requireSameOrgOrSuperAdmin(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireSameOrgOrSuperAdminAllowsSuperAdminForAnyOrg() {
        authenticateAs(null, UserRole.ROLE_SUPER_ADMIN);

        service.requireSameOrgOrSuperAdmin(UUID.randomUUID());
    }

    @Test
    void requireSameOrgOrSuperAdminRejectsUserWithNoOrganization() {
        authenticateAs(null, UserRole.ROLE_CONSUMER);

        assertThatThrownBy(() -> service.requireSameOrgOrSuperAdmin(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireSameOrgOrSuperAdminRejectsWhenNoAuthenticationPresent() {
        assertThatThrownBy(() -> service.requireSameOrgOrSuperAdmin(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void isSuperAdminReflectsTheAuthenticatedRole() {
        authenticateAs(UUID.randomUUID(), UserRole.ROLE_WHOLESALER_ADMIN);
        assertThat(service.isSuperAdmin()).isFalse();

        SecurityContextHolder.clearContext();
        authenticateAs(null, UserRole.ROLE_SUPER_ADMIN);
        assertThat(service.isSuperAdmin()).isTrue();
    }
}
