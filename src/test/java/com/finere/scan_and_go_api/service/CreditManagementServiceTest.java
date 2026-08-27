package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.CreditAccount;
import com.finere.scan_and_go_api.domain.entity.Invoice;
import com.finere.scan_and_go_api.domain.entity.Order;
import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.enums.CreditStatus;
import com.finere.scan_and_go_api.domain.enums.InvoiceStatus;
import com.finere.scan_and_go_api.exception.CreditAccountBlockedException;
import com.finere.scan_and_go_api.exception.CreditLimitExceededException;
import com.finere.scan_and_go_api.repository.CreditAccountRepository;
import com.finere.scan_and_go_api.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditManagementServiceTest {

    @Mock
    private CreditAccountRepository creditAccountRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private CreditManagementService service;

    private final UUID sellerOrgId = UUID.randomUUID();
    private final UUID buyerOrgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreditManagementService(creditAccountRepository, invoiceRepository);
    }

    private CreditAccount account(CreditStatus status, BigDecimal limit, BigDecimal balance) {
        CreditAccount account = new CreditAccount();
        account.setId(UUID.randomUUID());
        account.setStatus(status);
        account.setCreditLimit(limit);
        account.setCurrentBalance(balance);
        return account;
    }

    @Test
    void allowsOrderWithinLimitOnGoodStandingAccount() {
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("20000"));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        CreditAccount result = service.validateOrderAgainstCredit(sellerOrgId, buyerOrgId, new BigDecimal("50000"));

        assertThat(result).isSameAs(account);
    }

    @Test
    void rejectsOrderThatWouldExceedCreditLimit() {
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("60000"));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.validateOrderAgainstCredit(sellerOrgId, buyerOrgId, new BigDecimal("50000")))
                .isInstanceOf(CreditLimitExceededException.class);
    }

    @Test
    void allowsOrderThatExactlyMeetsTheCreditLimit() {
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("50000"));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        assertThat(service.validateOrderAgainstCredit(sellerOrgId, buyerOrgId, new BigDecimal("50000")))
                .isSameAs(account);
    }

    @Test
    void rejectsOrderWhenAccountIsLocked() {
        CreditAccount account = account(CreditStatus.LOCKED, new BigDecimal("100000"), BigDecimal.ZERO);
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.validateOrderAgainstCredit(sellerOrgId, buyerOrgId, BigDecimal.TEN))
                .isInstanceOf(CreditAccountBlockedException.class);
    }

    @Test
    void rejectsOrderWhenAccountIsOverdue() {
        CreditAccount account = account(CreditStatus.OVERDUE, new BigDecimal("100000"), BigDecimal.ZERO);
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.validateOrderAgainstCredit(sellerOrgId, buyerOrgId, BigDecimal.TEN))
                .isInstanceOf(CreditAccountBlockedException.class);
    }

    @Test
    void rejectsOrderWhenNoCreditRelationshipExists() {
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateOrderAgainstCredit(sellerOrgId, buyerOrgId, BigDecimal.TEN))
                .isInstanceOf(CreditAccountBlockedException.class);
    }

    @Test
    void increaseBalanceAddsToExistingBalance() {
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("20000"));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        service.increaseBalance(sellerOrgId, buyerOrgId, new BigDecimal("15000"));

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("35000");
        verify(creditAccountRepository).save(account);
    }

    private Invoice invoiceDueDaysAgo(long daysAgo, UUID creditAccountSellerOrgId, UUID creditAccountBuyerOrgId) {
        Organization seller = new Organization();
        seller.setId(creditAccountSellerOrgId);
        Organization buyer = new Organization();
        buyer.setId(creditAccountBuyerOrgId);

        Order order = new Order();
        order.setSellerOrg(seller);
        order.setBuyerOrg(buyer);

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setDueDate(LocalDate.now().minusDays(daysAgo));
        invoice.setStatus(InvoiceStatus.UNPAID);
        return invoice;
    }

    @Test
    void sweepEscalatesToOverdueWithinLockTolerance() {
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("50000"));
        Invoice invoice = invoiceDueDaysAgo(3, sellerOrgId, buyerOrgId);

        when(invoiceRepository.findByStatusInAndDueDateBefore(any(), any())).thenReturn(List.of(invoice));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        service.runNightlyOverdueSweep();

        assertThat(account.getStatus()).isEqualTo(CreditStatus.OVERDUE);
        verify(creditAccountRepository).save(account);
    }

    @Test
    void sweepEscalatesToLockedPastTolerance() {
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("50000"));
        Invoice invoice = invoiceDueDaysAgo(10, sellerOrgId, buyerOrgId);

        when(invoiceRepository.findByStatusInAndDueDateBefore(any(), any())).thenReturn(List.of(invoice));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        service.runNightlyOverdueSweep();

        assertThat(account.getStatus()).isEqualTo(CreditStatus.LOCKED);
    }

    @Test
    void sweepDoesNotEscalateExactlyAtToleranceBoundary() {
        // 7 days overdue is still "not > 7", so escalation to LOCKED should not happen yet.
        CreditAccount account = account(CreditStatus.GOOD_STANDING, new BigDecimal("100000"), new BigDecimal("50000"));
        Invoice invoice = invoiceDueDaysAgo(7, sellerOrgId, buyerOrgId);

        when(invoiceRepository.findByStatusInAndDueDateBefore(any(), any())).thenReturn(List.of(invoice));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        service.runNightlyOverdueSweep();

        assertThat(account.getStatus()).isEqualTo(CreditStatus.OVERDUE);
    }

    @Test
    void sweepDoesNotTouchAccountsWithNoOverdueInvoices() {
        when(invoiceRepository.findByStatusInAndDueDateBefore(any(), any())).thenReturn(List.of());

        service.runNightlyOverdueSweep();

        verify(creditAccountRepository, never()).save(any());
    }

    @Test
    void sweepDoesNotResaveWhenStatusUnchanged() {
        // Already LOCKED and still overdue - escalate() recomputes LOCKED again, so status
        // is unchanged and no redundant save should happen.
        CreditAccount account = account(CreditStatus.LOCKED, new BigDecimal("100000"), new BigDecimal("50000"));
        Invoice invoice = invoiceDueDaysAgo(15, sellerOrgId, buyerOrgId);

        when(invoiceRepository.findByStatusInAndDueDateBefore(any(), any())).thenReturn(List.of(invoice));
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(sellerOrgId, buyerOrgId))
                .thenReturn(Optional.of(account));

        service.runNightlyOverdueSweep();

        verify(creditAccountRepository, never()).save(any());
    }
}
