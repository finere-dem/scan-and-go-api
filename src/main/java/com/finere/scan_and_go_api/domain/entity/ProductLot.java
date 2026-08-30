package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.LotStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * current_quantity decrements are protected by a pessimistic row lock
 * (SELECT ... FOR UPDATE) taken in InventoryAllocationService, not by
 * JPA optimistic versioning.
 */
@Entity
@Table(name = "product_lots")
@Getter
@Setter
public class ProductLot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Exactly one of warehouse/boutique is set - a lot lives in a storage depot
     * (seller-side stock) or in a shop (buyer-side stock received from an order), never both. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boutique_id")
    private Boutique boutique;

    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate mfgDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expDate;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "unit_cost")
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LotStatus status = LotStatus.ACTIVE;
}
