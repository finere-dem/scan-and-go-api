package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    List<Warehouse> findByOrganizationId(UUID orgId);
}
