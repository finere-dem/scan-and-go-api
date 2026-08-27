package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.CreditAccount;
import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.dto.pricing.CreditAccountRequest;
import com.finere.scan_and_go_api.dto.pricing.CreditAccountResponse;
import com.finere.scan_and_go_api.repository.CreditAccountRepository;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditAccountServiceTest {

    @Mock private CreditAccountRepository creditAccountRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private CreditAccountService service;

    private final UUID creditorOrgId = UUID.randomUUID();
    private final UUID debtorOrgId = UUID.randomUUID();

    private Organization org(UUID id) {
        Organization organization = new Organization();
        organization.setId(id);
        return organization;
    }

    @BeforeEach
    void setUp() {
        lenient().when(organizationRepository.findById(creditorOrgId)).thenReturn(Optional.of(org(creditorOrgId)));
        lenient().when(organizationRepository.findById(debtorOrgId)).thenReturn(Optional.of(org(debtorOrgId)));
        lenient().when(creditAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30, 60, 90})
    void acceptsEachAllowedPaymentTerm(int termDays) {
        CreditAccountRequest request = new CreditAccountRequest(
                creditorOrgId, debtorOrgId, new BigDecimal("100000"), termDays);

        CreditAccountResponse response = service.create(request);

        assertThat(response.paymentTermDays()).isEqualTo(termDays);
        verify(currentUserService).requireSameOrgOrSuperAdmin(creditorOrgId);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 15, 45, 91, -30})
    void rejectsAnyPaymentTermNotInTheAllowedSet(int termDays) {
        CreditAccountRequest request = new CreditAccountRequest(
                creditorOrgId, debtorOrgId, new BigDecimal("100000"), termDays);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void validatesTermDaysBeforeCheckingOrgOwnership() {
        // Term-day validation is a pure input check and should fail fast, before the
        // (comparatively expensive) org-scoping check even runs.
        CreditAccountRequest request = new CreditAccountRequest(creditorOrgId, debtorOrgId, new BigDecimal("1"), 45);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void onlyTheCreditorOrgMayOpenTheCreditLineNotTheDebtor() {
        doThrow(new AccessDeniedException("not your org"))
                .when(currentUserService).requireSameOrgOrSuperAdmin(creditorOrgId);

        CreditAccountRequest request = new CreditAccountRequest(creditorOrgId, debtorOrgId, new BigDecimal("1"), 30);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(organizationRepository, creditAccountRepository);
    }

    @Test
    void rejectsUnknownDebtorOrganization() {
        when(organizationRepository.findById(debtorOrgId)).thenReturn(Optional.empty());
        CreditAccountRequest request = new CreditAccountRequest(creditorOrgId, debtorOrgId, new BigDecimal("1"), 30);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listByCreditorMapsEachAccountToItsResponse() {
        CreditAccount account = new CreditAccount();
        account.setId(UUID.randomUUID());
        account.setCreditorOrg(org(creditorOrgId));
        account.setDebtorOrg(org(debtorOrgId));
        account.setCreditLimit(new BigDecimal("50000"));
        when(creditAccountRepository.findByCreditorOrgId(creditorOrgId)).thenReturn(java.util.List.of(account));

        var result = service.listByCreditor(creditorOrgId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).creditorOrgId()).isEqualTo(creditorOrgId);
        assertThat(result.get(0).debtorOrgId()).isEqualTo(debtorOrgId);
    }

    @Test
    void getByCreditorAndDebtorThrowsWhenNoRelationshipExists() {
        when(creditAccountRepository.findByCreditorOrgIdAndDebtorOrgId(creditorOrgId, debtorOrgId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCreditorAndDebtor(creditorOrgId, debtorOrgId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
