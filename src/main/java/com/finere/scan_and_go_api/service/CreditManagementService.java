package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.CreditAccount;
import com.finere.scan_and_go_api.domain.entity.Invoice;
import com.finere.scan_and_go_api.domain.enums.CreditStatus;
import com.finere.scan_and_go_api.domain.enums.InvoiceStatus;
import com.finere.scan_and_go_api.exception.CreditAccountBlockedException;
import com.finere.scan_and_go_api.exception.CreditLimitExceededException;
import com.finere.scan_and_go_api.repository.CreditAccountRepository;
import com.finere.scan_and_go_api.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditManagementService {

    private static final Logger log = LoggerFactory.getLogger(CreditManagementService.class);

    /** Days past due_date before an OVERDUE account is escalated to LOCKED. */
    private static final int LOCK_TOLERANCE_DAYS = 7;

    private final CreditAccountRepository creditAccountRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Validates a prospective CREDIT order before it is confirmed.
     * Throws {@link CreditAccountBlockedException} if the account is not GOOD_STANDING,
     * or {@link CreditLimitExceededException} if the order would breach the credit limit.
     */
    @Transactional(readOnly = true)
    public CreditAccount validateOrderAgainstCredit(UUID sellerOrgId, UUID buyerOrgId, BigDecimal orderTotal) {
        CreditAccount account = creditAccountRepository
                .findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId)
                .orElseThrow(() -> new CreditAccountBlockedException(
                        "No credit relationship configured between seller " + sellerOrgId + " and buyer " + buyerOrgId));

        if (account.getStatus() != CreditStatus.GOOD_STANDING) {
            throw new CreditAccountBlockedException(
                    "Credit account is " + account.getStatus() + " for buyer " + buyerOrgId);
        }

        BigDecimal projectedBalance = account.getCurrentBalance().add(orderTotal);
        if (projectedBalance.compareTo(account.getCreditLimit()) > 0) {
            throw new CreditLimitExceededException(
                    "Order total " + orderTotal + " would exceed credit limit " + account.getCreditLimit()
                            + " (current balance " + account.getCurrentBalance() + ")");
        }

        return account;
    }

    @Transactional
    public void increaseBalance(UUID creditorOrgId, UUID debtorOrgId, BigDecimal amount) {
        CreditAccount account = creditAccountRepository
                .findByCreditorOrgIdAndDebtorOrgId(creditorOrgId, debtorOrgId)
                .orElseThrow(() -> new CreditAccountBlockedException(
                        "No credit relationship configured between " + creditorOrgId + " and " + debtorOrgId));
        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        account.setUpdatedAt(Instant.now());
        creditAccountRepository.save(account);
    }

    /**
     * Nightly batch (00:01) scanning overdue invoices and escalating the linked credit account's
     * status: GOOD_STANDING/OVERDUE -> OVERDUE if past due_date, then -> LOCKED once the overdue
     * period exceeds {@link #LOCK_TOLERANCE_DAYS}. A LOCKED account rejects new CREDIT orders with
     * CREDIT_ACCOUNT_LOCKED (see {@link #validateOrderAgainstCredit}).
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void runNightlyOverdueSweep() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusInAndDueDateBefore(
                List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID), today);

        log.info("Nightly credit sweep: {} overdue invoice(s) found", overdueInvoices.size());

        for (Invoice invoice : overdueInvoices) {
            UUID buyerOrgId = invoice.getOrder().getBuyerOrg().getId();
            UUID sellerOrgId = invoice.getOrder().getSellerOrg().getId();

            creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId)
                    .ifPresent(account -> escalate(account, invoice.getDueDate(), today));
        }
    }

    private void escalate(CreditAccount account, LocalDate dueDate, LocalDate today) {
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);

        CreditStatus previousStatus = account.getStatus();
        if (daysOverdue > LOCK_TOLERANCE_DAYS) {
            account.setStatus(CreditStatus.LOCKED);
        } else if (daysOverdue > 0) {
            account.setStatus(CreditStatus.OVERDUE);
        }

        if (account.getStatus() != previousStatus) {
            account.setUpdatedAt(Instant.now());
            creditAccountRepository.save(account);
            log.warn("Credit account {} escalated from {} to {} ({} day(s) overdue)",
                    account.getId(), previousStatus, account.getStatus(), daysOverdue);
        }
    }
}
