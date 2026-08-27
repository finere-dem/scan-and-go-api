package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.dto.organization.OrganizationRequest;
import com.finere.scan_and_go_api.dto.organization.OrganizationResponse;
import com.finere.scan_and_go_api.dto.organization.OrganizationStatusUpdateRequest;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setTaxId(request.taxId());
        organization.setRccm(request.rccm());
        organization.setOrgType(request.orgType());
        organization.setPhone(request.phone());
        organization.setEmail(request.email());
        organization.setAddress(request.address());
        return toResponse(organizationRepository.save(organization));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID id) {
        return organizationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + id));
    }

    /** KYC decision: SUPER_ADMIN moves an org between PENDING_KYC, ACTIVE, and SUSPENDED. */
    @Transactional
    public OrganizationResponse updateStatus(UUID id, OrganizationStatusUpdateRequest request) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + id));
        organization.setStatus(request.status());
        return toResponse(organizationRepository.save(organization));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listAll() {
        return organizationRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getTaxId(),
                organization.getRccm(),
                organization.getOrgType(),
                organization.getStatus(),
                organization.getPhone(),
                organization.getEmail(),
                organization.getAddress());
    }
}
