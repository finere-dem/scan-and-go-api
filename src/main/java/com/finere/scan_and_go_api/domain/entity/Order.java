package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.OrderStatus;
import com.finere.scan_and_go_api.domain.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_org_id", nullable = false)
    private Organization buyerOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_org_id", nullable = false)
    private Organization sellerOrg;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus = OrderStatus.DRAFT;

    /** Client-generated UUID used for idempotent offline-first sync (Outbox Pattern). */
    @Column(name = "client_sync_id", unique = true)
    private UUID clientSyncId;

    /** Where this order's goods were received into the buyer's own stock, if at all -
     * at most one of the two is set. Null means the buyer didn't ask to receive stock locally. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_warehouse_id")
    private Warehouse receivingWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_boutique_id")
    private Boutique receivingBoutique;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
