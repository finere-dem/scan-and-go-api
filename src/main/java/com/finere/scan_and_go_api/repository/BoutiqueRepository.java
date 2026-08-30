package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoutiqueRepository extends JpaRepository<Boutique, UUID> {
    List<Boutique> findByOrganizationId(UUID orgId);
}
