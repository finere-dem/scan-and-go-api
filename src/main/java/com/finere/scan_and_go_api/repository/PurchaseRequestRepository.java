package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.PurchaseRequest;
import com.finere.scan_and_go_api.domain.enums.PurchaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {
    List<PurchaseRequest> findByBuyerOrgId(UUID buyerOrgId);
    List<PurchaseRequest> findByBuyerOrgIdAndStatus(UUID buyerOrgId, PurchaseRequestStatus status);
}
