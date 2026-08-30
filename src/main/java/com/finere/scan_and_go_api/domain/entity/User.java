package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRole role;

    /** Set only for staff scoped to a single depot (ROLE_LOGISTICS_OPERATOR) rather than the whole org. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_warehouse_id")
    private Warehouse assignedWarehouse;

    /** Set only for staff scoped to a single shop (ROLE_BOUTIQUE_STAFF) rather than the whole org. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_boutique_id")
    private Boutique assignedBoutique;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
