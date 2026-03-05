package com.finere.scan_and_go_api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Order() {
    }

    public Order(Long id, Double totalAmount, String status, LocalDateTime createdAt, Store store) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.store = store;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }
}
