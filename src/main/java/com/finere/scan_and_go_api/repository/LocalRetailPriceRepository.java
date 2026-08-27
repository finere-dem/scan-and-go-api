package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.LocalRetailPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalRetailPriceRepository extends JpaRepository<LocalRetailPrice, UUID> {
    Optional<LocalRetailPrice> findByRetailerOrgIdAndProductId(UUID retailerOrgId, UUID productId);
    List<LocalRetailPrice> findByRetailerOrgId(UUID retailerOrgId);
}
