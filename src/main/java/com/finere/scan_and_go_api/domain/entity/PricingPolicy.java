package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.TargetOrgType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pricing_policies")
@Getter
@Setter
public class PricingPolicy {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_org_id", nullable = false)
    private Organization sellerOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_org_type", nullable = false, length = 30)
    private TargetOrgType targetOrgType;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "min_order_qty", nullable = false)
    private Integer minOrderQty = 1;

    @Column(nullable = false, length = 10)
    private String currency = "XOF";
}
