package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.PaymentMode;
import com.finere.scan_and_go_api.domain.enums.PurchaseRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A purchase a PURCHASE_STAFF account wants to make, awaiting approval from the
 * org's owner before it becomes a real {@link Order} - see PurchaseRequestService. */
@Entity
@Table(name = "purchase_requests")
@Getter
@Setter
public class PurchaseRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_org_id", nullable = false)
    private Organization buyerOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_org_id", nullable = false)
    private Organization sellerOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_warehouse_id")
    private Warehouse receivingWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_boutique_id")
    private Boutique receivingBoutique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRequestStatus status = PurchaseRequestStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "resulting_order_id")
    private UUID resultingOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestItem> items = new ArrayList<>();
}
