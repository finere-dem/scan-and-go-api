package com.finere.scan_and_go_api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_retail_prices")
@Getter
@Setter
public class LocalRetailPrice {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_org_id", nullable = false)
    private Organization retailerOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "consumer_price", nullable = false)
    private BigDecimal consumerPrice;

    @Column(nullable = false, length = 10)
    private String currency = "XOF";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
