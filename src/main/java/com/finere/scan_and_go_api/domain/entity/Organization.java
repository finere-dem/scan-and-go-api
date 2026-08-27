package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.OrgStatus;
import com.finere.scan_and_go_api.domain.enums.OrgType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId;

    private String rccm;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, length = 30)
    private OrgType orgType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrgStatus status = OrgStatus.PENDING_KYC;

    private String phone;
    private String email;
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
