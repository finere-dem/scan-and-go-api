package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.Product;
import com.finere.scan_and_go_api.domain.entity.Warehouse;
import com.finere.scan_and_go_api.dto.product.ProductLotRequest;
import com.finere.scan_and_go_api.dto.product.ProductLotResponse;
import com.finere.scan_and_go_api.repository.ProductLotRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.repository.WarehouseRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLotServiceTest {

    @Mock private ProductLotRepository productLotRepository;
    @Mock private ProductRepository productRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private ProductLotService service;

    private final UUID productId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();
    private final UUID warehouseOrgId = UUID.randomUUID();

    private ProductLotRequest request(LocalDate mfgDate, LocalDate expDate) {
        return new ProductLotRequest(productId, warehouseId, "LOT-1", mfgDate, expDate, 10, null);
    }

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setId(productId);
        Organization warehouseOrg = new Organization();
        warehouseOrg.setId(warehouseOrgId);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setOrganization(warehouseOrg);

        lenient().when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        lenient().when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        lenient().when(productLotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rejectsExpiryDateNotAfterManufacturingDate() {
        LocalDate sameDay = LocalDate.now();

        assertThatThrownBy(() -> service.create(request(sameDay, sameDay)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expiry date must be after manufacturing date");
    }

    @Test
    void rejectsExpiryDateBeforeManufacturingDate() {
        assertThatThrownBy(() -> service.create(request(LocalDate.now(), LocalDate.now().minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateValidationHappensBeforeAnyLookup() {
        // A malformed date range is a pure input error and should fail before we even
        // bother resolving the product/warehouse from the database.
        LocalDate sameDay = LocalDate.now();

        assertThatThrownBy(() -> service.create(request(sameDay, sameDay)));

        verifyNoInteractions(productRepository, warehouseRepository, currentUserService);
    }

    @Test
    void createsLotWhenDatesAreValid() {
        ProductLotResponse response = service.create(request(LocalDate.now(), LocalDate.now().plusYears(1)));

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.warehouseId()).isEqualTo(warehouseId);
        assertThat(response.currentQuantity()).isEqualTo(10);
        assertThat(response.initialQuantity()).isEqualTo(10);
    }

    @Test
    void scopesCreationToTheWarehousesOwningOrgNotTheProductsImporter() {
        service.create(request(LocalDate.now(), LocalDate.now().plusYears(1)));

        verify(currentUserService).requireSameOrgOrSuperAdmin(warehouseOrgId);
    }

    @Test
    void rejectsWhenCallerDoesNotOwnTheWarehouse() {
        doThrow(new AccessDeniedException("not your warehouse"))
                .when(currentUserService).requireSameOrgOrSuperAdmin(warehouseOrgId);

        assertThatThrownBy(() -> service.create(request(LocalDate.now(), LocalDate.now().plusYears(1))))
                .isInstanceOf(AccessDeniedException.class);

        verify(productLotRepository, never()).save(any());
    }
}
