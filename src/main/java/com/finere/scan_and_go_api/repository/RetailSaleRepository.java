package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.RetailSale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetailSaleRepository extends JpaRepository<RetailSale, UUID> {
    List<RetailSale> findByBoutiqueIdOrderByCreatedAtDesc(UUID boutiqueId);
}
