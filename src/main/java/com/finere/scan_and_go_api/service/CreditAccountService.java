package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.CreditAccount;
import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.dto.audit.CreditAccountRevisionResponse;
import com.finere.scan_and_go_api.dto.pricing.CreditAccountRequest;
import com.finere.scan_and_go_api.dto.pricing.CreditAccountResponse;
import com.finere.scan_and_go_api.repository.CreditAccountRepository;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditAccountService {

    private static final Set<Integer> ALLOWED_TERM_DAYS = Set.of(0, 30, 60, 90);

    private final CreditAccountRepository creditAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    @Transactional
    public CreditAccountResponse create(CreditAccountRequest request) {
        if (!ALLOWED_TERM_DAYS.contains(request.paymentTermDays())) {
            throw new IllegalArgumentException("paymentTermDays must be one of 0, 30, 60, 90");
        }

        // Only the creditor org (the seller granting terms) may open a credit line - not the debtor.
        currentUserService.requireSameOrgOrSuperAdmin(request.creditorOrgId());

        Organization creditorOrg = organizationRepository.findById(request.creditorOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown creditor organization: " + request.creditorOrgId()));
        Organization debtorOrg = organizationRepository.findById(request.debtorOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown debtor organization: " + request.debtorOrgId()));

        CreditAccount account = new CreditAccount();
        account.setCreditorOrg(creditorOrg);
        account.setDebtorOrg(debtorOrg);
        account.setCreditLimit(request.creditLimit());
        account.setPaymentTermDays(request.paymentTermDays());

        return toResponse(creditAccountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public CreditAccountResponse getByCreditorAndDebtor(UUID creditorOrgId, UUID debtorOrgId) {
        return creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(creditorOrgId, debtorOrgId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No credit account between " + creditorOrgId + " and " + debtorOrgId));
    }

    @Transactional(readOnly = true)
    public List<CreditAccountResponse> listByCreditor(UUID creditorOrgId) {
        return creditAccountRepository.findByCreditorOrgId(creditorOrgId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<CreditAccountRevisionResponse> listRevisions(UUID creditAccountId) {
        CreditAccount current = creditAccountRepository.findById(creditAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown credit account: " + creditAccountId));
        currentUserService.requireSameOrgOrSuperAdmin(current.getCreditorOrg().getId());

        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Object[]> results = auditReader.createQuery()
                .forRevisionsOfEntity(CreditAccount.class, false, true)
                .add(org.hibernate.envers.query.AuditEntity.id().eq(creditAccountId))
                .getResultList();

        return results.stream()
                .map(row -> {
                    CreditAccount snapshot = (CreditAccount) row[0];
                    DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) row[1];
                    RevisionType revisionType = (RevisionType) row[2];
                    return new CreditAccountRevisionResponse(
                            revisionEntity.getId(),
                            Instant.ofEpochMilli(revisionEntity.getTimestamp()),
                            revisionType.name(),
                            snapshot.getCreditLimit(),
                            snapshot.getCurrentBalance(),
                            snapshot.getStatus());
                })
                .toList();
    }

    private CreditAccountResponse toResponse(CreditAccount account) {
        return new CreditAccountResponse(
                account.getId(),
                account.getCreditorOrg().getId(),
                account.getDebtorOrg().getId(),
                account.getCreditLimit(),
                account.getCurrentBalance(),
                account.getPaymentTermDays(),
                account.getStatus());
    }
}
