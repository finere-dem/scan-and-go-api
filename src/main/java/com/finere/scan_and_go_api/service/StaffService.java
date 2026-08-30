package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Boutique;
import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.User;
import com.finere.scan_and_go_api.domain.entity.Warehouse;
import com.finere.scan_and_go_api.domain.enums.UserRole;
import com.finere.scan_and_go_api.dto.organization.StaffCreateRequest;
import com.finere.scan_and_go_api.dto.organization.StaffResponse;
import com.finere.scan_and_go_api.repository.BoutiqueRepository;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.repository.UserRepository;
import com.finere.scan_and_go_api.repository.WarehouseRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final Set<UserRole> ASSIGNABLE_STAFF_ROLES = Set.of(UserRole.ROLE_LOGISTICS_OPERATOR, UserRole.ROLE_BOUTIQUE_STAFF);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final WarehouseRepository warehouseRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional
    public StaffResponse create(UUID orgId, StaffCreateRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(orgId);

        if (!ASSIGNABLE_STAFF_ROLES.contains(request.role())) {
            throw new IllegalArgumentException(
                    "An organization can only create staff with role LOGISTICS_OPERATOR or BOUTIQUE_STAFF, not " + request.role());
        }
        if (userRepository.findByPhone(request.phone()).isPresent()) {
            throw new BadCredentialsException("A user already exists with this phone number");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + orgId));

        Warehouse assignedWarehouse = null;
        if (request.assignedWarehouseId() != null) {
            assignedWarehouse = warehouseRepository.findById(request.assignedWarehouseId())
                    .filter(w -> w.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new IllegalArgumentException("Warehouse does not belong to this organization"));
        }

        Boutique assignedBoutique = null;
        if (request.assignedBoutiqueId() != null) {
            assignedBoutique = boutiqueRepository.findById(request.assignedBoutiqueId())
                    .filter(b -> b.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new IllegalArgumentException("Boutique does not belong to this organization"));
        }

        User user = new User();
        user.setOrganization(org);
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        user.setAssignedWarehouse(assignedWarehouse);
        user.setAssignedBoutique(assignedBoutique);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> listByOrganization(UUID orgId) {
        currentUserService.requireSameOrgOrSuperAdmin(orgId);
        return userRepository.findByOrganizationId(orgId).stream()
                .filter(u -> ASSIGNABLE_STAFF_ROLES.contains(u.getRole()))
                .map(this::toResponse)
                .toList();
    }

    private StaffResponse toResponse(User user) {
        return new StaffResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getPhone(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getAssignedWarehouse() != null ? user.getAssignedWarehouse().getId() : null,
                user.getAssignedBoutique() != null ? user.getAssignedBoutique().getId() : null,
                user.isActive());
    }
}
