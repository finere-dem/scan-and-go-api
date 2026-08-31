package com.finere.scan_and_go_api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A checkout sale to a walk-in consumer at a boutique - the POS transaction,
 * distinct from the B2B Order/PurchaseRequest (organization buying from organization). */
@Entity
@Table(name = "retail_sales")
@Getter
@Setter
public class RetailSale {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sold_by_user_id", nullable = false)
    private User soldBy;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "retailSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RetailSaleItem> items = new ArrayList<>();
}
