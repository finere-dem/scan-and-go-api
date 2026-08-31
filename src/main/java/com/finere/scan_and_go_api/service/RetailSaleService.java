package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.*;
import com.finere.scan_and_go_api.dto.order.LotAllocation;
import com.finere.scan_and_go_api.dto.pos.*;
import com.finere.scan_and_go_api.repository.*;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The POS/checkout flow: a boutique selling straight to a walk-in consumer. Stock is FEFO-
 * allocated straight out of the boutique's own lots (never a warehouse), which naturally blocks
 * both an over-sell (insufficient stock throws) and selling an expired lot (the FEFO query only
 * returns non-expired lots) without any extra check needed here.
 */
@Service
@RequiredArgsConstructor
public class RetailSaleService {

    private final RetailSaleRepository retailSaleRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final ProductRepository productRepository;
    private final ProductLotRepository productLotRepository;
    private final LocalRetailPriceRepository localRetailPriceRepository;
    private final UserRepository userRepository;
    private final InventoryAllocationService inventoryAllocationService;
    private final CurrentUserService currentUserService;

    @Transactional
    public RetailSaleResponse createSale(RetailSaleCreateRequest request) {
        Boutique boutique = boutiqueRepository.findById(request.boutiqueId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown boutique: " + request.boutiqueId()));
        currentUserService.requireSameOrgOrSuperAdmin(boutique.getOrganization().getId());

        User soldBy = userRepository.getReferenceById(currentUserService.requireUserId());

        RetailSale sale = new RetailSale();
        sale.setBoutique(boutique);
        sale.setSoldBy(soldBy);

        BigDecimal total = BigDecimal.ZERO;
        List<LotAllocation> allAllocationsSoFar = new ArrayList<>();

        try {
            for (RetailSaleItemInput itemInput : request.items()) {
                Product product = productRepository.findById(itemInput.productId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + itemInput.productId()));

                BigDecimal unitPrice = localRetailPriceRepository
                        .findByRetailerOrgIdAndProductId(boutique.getOrganization().getId(), product.getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No consumer price set for product " + product.getId() + " - set it in Retarification first"))
                        .getConsumerPrice();

                List<LotAllocation> allocations = inventoryAllocationService.allocateStockAtBoutique(
                        boutique.getId(), product.getId(), itemInput.quantity());
                allAllocationsSoFar.addAll(allocations);

                for (LotAllocation allocation : allocations) {
                    RetailSaleItem item = new RetailSaleItem();
                    item.setRetailSale(sale);
                    item.setProduct(product);
                    item.setLot(productLotRepository.getReferenceById(allocation.lotId()));
                    item.setQuantity(allocation.quantity());
                    item.setUnitPrice(unitPrice);
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(allocation.quantity()));
                    item.setSubtotal(subtotal);
                    sale.getItems().add(item);
                    total = total.add(subtotal);
                }
            }
        } catch (RuntimeException e) {
            if (!allAllocationsSoFar.isEmpty()) {
                inventoryAllocationService.releaseAllocations(allAllocationsSoFar);
            }
            throw e;
        }

        sale.setTotalAmount(total);
        return toResponse(retailSaleRepository.save(sale));
    }

    @Transactional(readOnly = true)
    public List<RetailSaleResponse> listByBoutique(UUID boutiqueId) {
        return retailSaleRepository.findByBoutiqueIdOrderByCreatedAtDesc(boutiqueId).stream().map(this::toResponse).toList();
    }

    private RetailSaleResponse toResponse(RetailSale sale) {
        List<RetailSaleItemResponse> items = sale.getItems().stream()
                .map(item -> new RetailSaleItemResponse(
                        item.getProduct().getId(), item.getLot().getId(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .toList();

        return new RetailSaleResponse(
                sale.getId(), sale.getBoutique().getId(), sale.getSoldBy().getId(), sale.getTotalAmount(), sale.getCreatedAt(), items);
    }
}
