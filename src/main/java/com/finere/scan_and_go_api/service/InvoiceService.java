package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Invoice;
import com.finere.scan_and_go_api.dto.invoice.InvoiceResponse;
import com.finere.scan_and_go_api.repository.InvoiceRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listByBuyer(UUID buyerOrgId) {
        currentUserService.requireSameOrgOrSuperAdmin(buyerOrgId);
        return invoiceRepository.findByOrder_BuyerOrgId(buyerOrgId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listBySeller(UUID sellerOrgId) {
        currentUserService.requireSameOrgOrSuperAdmin(sellerOrgId);
        return invoiceRepository.findByOrder_SellerOrgId(sellerOrgId).stream().map(this::toResponse).toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrder().getId(),
                invoice.getOrder().getOrderNumber(),
                invoice.getOrder().getBuyerOrg().getId(),
                invoice.getOrder().getSellerOrg().getId(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getAmountDue(),
                invoice.getAmountPaid(),
                invoice.getStatus());
    }
}
