package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.CreditStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Audited via Hibernate Envers: every balance/status change (credit granted, order-driven
 * increase, nightly overdue-sweep escalation) is dispute-sensitive, so a full revision
 * history is kept in credit_accounts_aud rather than only the latest snapshot.
 */
@Entity
@Table(name = "credit_accounts")
@Audited
@Getter
@Setter
public class CreditAccount {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creditor_org_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Organization creditorOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debtor_org_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Organization debtorOrg;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "payment_term_days", nullable = false)
    private Integer paymentTermDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditStatus status = CreditStatus.GOOD_STANDING;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
