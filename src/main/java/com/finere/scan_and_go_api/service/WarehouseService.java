package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.Warehouse;
import com.finere.scan_and_go_api.dto.organization.WarehouseRequest;
import com.finere.scan_and_go_api.dto.organization.WarehouseResponse;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.repository.WarehouseRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(request.orgId());

        Organization org = organizationRepository.findById(request.orgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + request.orgId()));

        Warehouse warehouse = new Warehouse();
        warehouse.setOrganization(org);
        warehouse.setName(request.name());
        warehouse.setCode(request.code());
        warehouse.setAddress(request.address());
        warehouse.setLatitude(request.latitude());
        warehouse.setLongitude(request.longitude());

        return toResponse(warehouseRepository.save(warehouse));
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> listByOrganization(UUID orgId) {
        return warehouseRepository.findByOrganizationId(orgId).stream().map(this::toResponse).toList();
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getOrganization().getId(),
                warehouse.getName(),
                warehouse.getCode(),
                warehouse.getAddress(),
                warehouse.getLatitude(),
                warehouse.getLongitude(),
                warehouse.isActive());
    }
}
