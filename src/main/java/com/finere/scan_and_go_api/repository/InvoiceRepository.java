package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.Invoice;
import com.finere.scan_and_go_api.domain.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByOrderId(UUID orderId);

    List<Invoice> findByStatusInAndDueDateBefore(List<InvoiceStatus> statuses, LocalDate date);

    List<Invoice> findByOrder_BuyerOrgId(UUID buyerOrgId);

    List<Invoice> findByOrder_SellerOrgId(UUID sellerOrgId);
}
