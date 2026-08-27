package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByClientSyncId(UUID clientSyncId);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByBuyerOrgId(UUID buyerOrgId);
    List<Order> findBySellerOrgId(UUID sellerOrgId);
}
