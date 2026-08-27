package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.PricingPolicy;
import com.finere.scan_and_go_api.domain.enums.TargetOrgType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, UUID> {
    Optional<PricingPolicy> findBySellerOrgIdAndProductIdAndTargetOrgType(
            UUID sellerOrgId, UUID productId, TargetOrgType targetOrgType);

    List<PricingPolicy> findBySellerOrgId(UUID sellerOrgId);
}
