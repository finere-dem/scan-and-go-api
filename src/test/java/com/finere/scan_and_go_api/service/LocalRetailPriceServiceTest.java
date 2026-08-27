package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.LocalRetailPrice;
import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.Product;
import com.finere.scan_and_go_api.dto.pricing.LocalRetailPriceRequest;
import com.finere.scan_and_go_api.dto.pricing.LocalRetailPriceResponse;
import com.finere.scan_and_go_api.repository.LocalRetailPriceRepository;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalRetailPriceServiceTest {

    @Mock private LocalRetailPriceRepository localRetailPriceRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private LocalRetailPriceService service;

    private final UUID retailerOrgId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @Test
    void createsANewPriceWhenNoneExistsYet() {
        when(localRetailPriceRepository.findByRetailerOrgIdAndProductId(retailerOrgId, productId))
                .thenReturn(Optional.empty());
        Organization retailerOrg = new Organization();
        retailerOrg.setId(retailerOrgId);
        Product product = new Product();
        product.setId(productId);
        when(organizationRepository.findById(retailerOrgId)).thenReturn(Optional.of(retailerOrg));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(localRetailPriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalRetailPriceRequest request = new LocalRetailPriceRequest(retailerOrgId, productId, new BigDecimal("1500"), "XOF");
        LocalRetailPriceResponse response = service.upsert(request);

        assertThat(response.retailerOrgId()).isEqualTo(retailerOrgId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.consumerPrice()).isEqualByComparingTo("1500");
    }

    @Test
    void updatesTheExistingPriceInPlaceRatherThanCreatingADuplicate() {
        LocalRetailPrice existing = new LocalRetailPrice();
        existing.setId(UUID.randomUUID());
        Organization retailerOrg = new Organization();
        retailerOrg.setId(retailerOrgId);
        Product product = new Product();
        product.setId(productId);
        existing.setRetailerOrg(retailerOrg);
        existing.setProduct(product);
        existing.setConsumerPrice(new BigDecimal("1000"));
        Instant originalUpdatedAt = Instant.now().minusSeconds(3600);
        existing.setUpdatedAt(originalUpdatedAt);

        when(localRetailPriceRepository.findByRetailerOrgIdAndProductId(retailerOrgId, productId))
                .thenReturn(Optional.of(existing));
        when(localRetailPriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalRetailPriceRequest request = new LocalRetailPriceRequest(retailerOrgId, productId, new BigDecimal("1800"), null);
        LocalRetailPriceResponse response = service.upsert(request);

        assertThat(response.consumerPrice()).isEqualByComparingTo("1800");
        assertThat(existing.getUpdatedAt()).isAfter(originalUpdatedAt);
        // No new entity created - the same id carries through since we mutated the existing row.
        assertThat(response.id()).isEqualTo(existing.getId());
        verifyNoInteractions(organizationRepository, productRepository);
    }

    @Test
    void nullOrBlankCurrencyOnUpdateLeavesExistingCurrencyUnchanged() {
        LocalRetailPrice existing = new LocalRetailPrice();
        existing.setId(UUID.randomUUID());
        Organization retailerOrg = new Organization();
        retailerOrg.setId(retailerOrgId);
        Product product = new Product();
        product.setId(productId);
        existing.setRetailerOrg(retailerOrg);
        existing.setProduct(product);
        existing.setCurrency("XOF");

        when(localRetailPriceRepository.findByRetailerOrgIdAndProductId(retailerOrgId, productId))
                .thenReturn(Optional.of(existing));
        when(localRetailPriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalRetailPriceRequest request = new LocalRetailPriceRequest(retailerOrgId, productId, new BigDecimal("2000"), "  ");
        service.upsert(request);

        assertThat(existing.getCurrency()).isEqualTo("XOF");
    }

    @Test
    void rejectsWhenCallerDoesNotOwnTheRetailerOrg() {
        doThrow(new AccessDeniedException("not your org"))
                .when(currentUserService).requireSameOrgOrSuperAdmin(retailerOrgId);

        LocalRetailPriceRequest request = new LocalRetailPriceRequest(retailerOrgId, productId, new BigDecimal("1"), null);

        assertThatThrownBy(() -> service.upsert(request)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(localRetailPriceRepository);
    }

    @Test
    void listByRetailerEnforcesOrgScopingBeforeQuerying() {
        doThrow(new AccessDeniedException("not your org"))
                .when(currentUserService).requireSameOrgOrSuperAdmin(retailerOrgId);

        assertThatThrownBy(() -> service.listByRetailer(retailerOrgId)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(localRetailPriceRepository);
    }
}
