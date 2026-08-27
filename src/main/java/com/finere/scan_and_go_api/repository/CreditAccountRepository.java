package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.CreditAccount;
import com.finere.scan_and_go_api.domain.enums.CreditStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, UUID> {
    Optional<CreditAccount> findByCreditorOrgIdAndDebtorOrgId(UUID creditorOrgId, UUID debtorOrgId);
    List<CreditAccount> findByStatus(CreditStatus status);
    List<CreditAccount> findByCreditorOrgId(UUID creditorOrgId);
}
