package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.ProductLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductLotRepository extends JpaRepository<ProductLot, UUID> {

    List<ProductLot> findByWarehouseId(UUID warehouseId);

    List<ProductLot> findByBoutiqueId(UUID boutiqueId);

    List<ProductLot> findByProductId(UUID productId);

    /** Used when receiving an order's goods: increments an existing lot of the same batch
     * rather than creating a duplicate row if this exact lot was already received before. */
    Optional<ProductLot> findByProductIdAndWarehouseIdAndLotNumber(UUID productId, UUID warehouseId, String lotNumber);

    Optional<ProductLot> findByProductIdAndBoutiqueIdAndLotNumber(UUID productId, UUID boutiqueId, String lotNumber);

    /**
     * FEFO candidates: active, non-expired, in-stock lots for the given product/warehouse,
     * oldest expiry first. Locked FOR UPDATE so concurrent allocations serialize on these rows.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM ProductLot l
            WHERE l.product.id = :productId
              AND l.warehouse.id = :warehouseId
              AND l.status = com.finere.scan_and_go_api.domain.enums.LotStatus.ACTIVE
              AND l.currentQuantity > 0
              AND l.expDate > CURRENT_DATE
            ORDER BY l.expDate ASC
            """)
    List<ProductLot> findAllocatableLotsForUpdate(UUID productId, UUID warehouseId);
}
