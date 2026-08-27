package com.finere.scan_and_go_api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importer_id", nullable = false)
    private Organization importer;

    @Column(nullable = false)
    private String sku;

    @Column(length = 13)
    private String ean13;

    @Column(nullable = false)
    private String name;

    private String brand;
    private String category;

    @Column(name = "packaging_type")
    private String packagingType;

    @Column(name = "units_per_box")
    private Integer unitsPerBox;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
