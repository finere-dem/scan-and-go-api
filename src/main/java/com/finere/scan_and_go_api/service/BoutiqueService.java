package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Boutique;
import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.dto.organization.BoutiqueRequest;
import com.finere.scan_and_go_api.dto.organization.BoutiqueResponse;
import com.finere.scan_and_go_api.repository.BoutiqueRepository;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoutiqueService {

    private final BoutiqueRepository boutiqueRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public BoutiqueResponse create(BoutiqueRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(request.orgId());

        Organization org = organizationRepository.findById(request.orgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + request.orgId()));

        Boutique boutique = new Boutique();
        boutique.setOrganization(org);
        boutique.setName(request.name());
        boutique.setCode(request.code());
        boutique.setAddress(request.address());

        return toResponse(boutiqueRepository.save(boutique));
    }

    @Transactional(readOnly = true)
    public List<BoutiqueResponse> listByOrganization(UUID orgId) {
        return boutiqueRepository.findByOrganizationId(orgId).stream().map(this::toResponse).toList();
    }

    private BoutiqueResponse toResponse(Boutique boutique) {
        return new BoutiqueResponse(
                boutique.getId(),
                boutique.getOrganization().getId(),
                boutique.getName(),
                boutique.getCode(),
                boutique.getAddress(),
                boutique.isActive());
    }
}
