package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.ProductLot;
import com.finere.scan_and_go_api.domain.enums.LotStatus;
import com.finere.scan_and_go_api.dto.order.LotAllocation;
import com.finere.scan_and_go_api.exception.InsufficientStockException;
import com.finere.scan_and_go_api.repository.ProductLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FEFO (First Expired, First Out) stock allocation.
 *
 * <p>Runs in its own REQUIRES_NEW transaction so the pessimistic row locks it takes
 * (via {@link ProductLotRepository#findAllocatableLotsForUpdate}) are held only for the
 * duration of the allocation itself, not the whole order-creation transaction — this keeps
 * the lock window as short as possible under concurrent order submission.
 */
@Service
@RequiredArgsConstructor
public class InventoryAllocationService {

    private final ProductLotRepository productLotRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<LotAllocation> allocateStock(UUID warehouseId, UUID productId, int requestedQuantity) {
        List<ProductLot> lots = productLotRepository.findAllocatableLotsForUpdate(productId, warehouseId);
        return allocateFrom(lots, productId, requestedQuantity, "warehouse " + warehouseId);
    }

    /** Same as {@link #allocateStock} but sells straight out of a boutique's own stock rather
     * than a depot - the FEFO allocation a POS/checkout sale needs. Also naturally blocks selling
     * an expired lot, since {@link ProductLotRepository#findAllocatableLotsForUpdateAtBoutique}
     * only returns lots whose expiry date is still in the future. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<LotAllocation> allocateStockAtBoutique(UUID boutiqueId, UUID productId, int requestedQuantity) {
        List<ProductLot> lots = productLotRepository.findAllocatableLotsForUpdateAtBoutique(productId, boutiqueId);
        return allocateFrom(lots, productId, requestedQuantity, "boutique " + boutiqueId);
    }

    private List<LotAllocation> allocateFrom(List<ProductLot> lots, UUID productId, int requestedQuantity, String locationDescription) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }

        List<LotAllocation> allocations = new ArrayList<>();
        int remaining = requestedQuantity;

        for (ProductLot lot : lots) {
            if (remaining <= 0) {
                break;
            }
            int takeFromLot = Math.min(remaining, lot.getCurrentQuantity());
            if (takeFromLot <= 0) {
                continue;
            }

            lot.setCurrentQuantity(lot.getCurrentQuantity() - takeFromLot);
            if (lot.getCurrentQuantity() == 0) {
                lot.setStatus(LotStatus.DEPLETED);
            }
            productLotRepository.save(lot);

            allocations.add(new LotAllocation(lot.getId(), takeFromLot));
            remaining -= takeFromLot;
        }

        if (remaining > 0) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + " in " + locationDescription
                            + ": missing " + remaining + " unit(s)");
        }

        return allocations;
    }

    /**
     * Compensating action for a multi-item order: each {@link #allocateStock} call commits
     * independently (REQUIRES_NEW), so if a LATER item in the same order fails, earlier
     * successful allocations must be explicitly given back rather than relying on rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseAllocations(List<LotAllocation> allocations) {
        for (LotAllocation allocation : allocations) {
            productLotRepository.findById(allocation.lotId()).ifPresent(lot -> {
                lot.setCurrentQuantity(lot.getCurrentQuantity() + allocation.quantity());
                lot.setStatus(LotStatus.ACTIVE);
                productLotRepository.save(lot);
            });
        }
    }
}
