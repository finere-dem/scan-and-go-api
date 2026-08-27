package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.ProductLot;
import com.finere.scan_and_go_api.domain.enums.LotStatus;
import com.finere.scan_and_go_api.dto.order.LotAllocation;
import com.finere.scan_and_go_api.exception.InsufficientStockException;
import com.finere.scan_and_go_api.repository.ProductLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAllocationServiceTest {

    @Mock
    private ProductLotRepository productLotRepository;

    private InventoryAllocationService service;

    private final UUID warehouseId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InventoryAllocationService(productLotRepository);
    }

    private ProductLot lot(LocalDate expDate, int currentQuantity) {
        ProductLot lot = new ProductLot();
        lot.setId(UUID.randomUUID());
        lot.setExpDate(expDate);
        lot.setCurrentQuantity(currentQuantity);
        lot.setInitialQuantity(currentQuantity);
        lot.setStatus(LotStatus.ACTIVE);
        return lot;
    }

    @Test
    void allocatesFromEarliestExpiringLotFirst() {
        ProductLot expiresSoon = lot(LocalDate.now().plusMonths(1), 10);
        ProductLot expiresLater = lot(LocalDate.now().plusMonths(6), 10);
        when(productLotRepository.findAllocatableLotsForUpdate(productId, warehouseId))
                .thenReturn(List.of(expiresSoon, expiresLater));

        List<LotAllocation> allocations = service.allocateStock(warehouseId, productId, 15);

        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).lotId()).isEqualTo(expiresSoon.getId());
        assertThat(allocations.get(0).quantity()).isEqualTo(10);
        assertThat(allocations.get(1).lotId()).isEqualTo(expiresLater.getId());
        assertThat(allocations.get(1).quantity()).isEqualTo(5);

        assertThat(expiresSoon.getCurrentQuantity()).isZero();
        assertThat(expiresSoon.getStatus()).isEqualTo(LotStatus.DEPLETED);
        assertThat(expiresLater.getCurrentQuantity()).isEqualTo(5);
        assertThat(expiresLater.getStatus()).isEqualTo(LotStatus.ACTIVE);
    }

    @Test
    void allocatesFullyFromASingleLotWhenSufficient() {
        ProductLot lot = lot(LocalDate.now().plusMonths(1), 20);
        when(productLotRepository.findAllocatableLotsForUpdate(productId, warehouseId)).thenReturn(List.of(lot));

        List<LotAllocation> allocations = service.allocateStock(warehouseId, productId, 5);

        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).quantity()).isEqualTo(5);
        assertThat(lot.getCurrentQuantity()).isEqualTo(15);
        assertThat(lot.getStatus()).isEqualTo(LotStatus.ACTIVE);
    }

    @Test
    void throwsInsufficientStockWhenTotalAcrossAllLotsIsNotEnough() {
        ProductLot lot = lot(LocalDate.now().plusMonths(1), 3);
        when(productLotRepository.findAllocatableLotsForUpdate(productId, warehouseId)).thenReturn(List.of(lot));

        assertThatThrownBy(() -> service.allocateStock(warehouseId, productId, 10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("missing 7");
    }

    @Test
    void throwsInsufficientStockWhenNoLotsAvailable() {
        when(productLotRepository.findAllocatableLotsForUpdate(productId, warehouseId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.allocateStock(warehouseId, productId, 1))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void rejectsNonPositiveRequestedQuantity() {
        assertThatThrownBy(() -> service.allocateStock(warehouseId, productId, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releaseAllocationsGivesQuantityBackAndReactivatesDepletedLot() {
        ProductLot lot = lot(LocalDate.now().plusMonths(1), 0);
        lot.setStatus(LotStatus.DEPLETED);
        UUID lotId = lot.getId();
        when(productLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        service.releaseAllocations(List.of(new LotAllocation(lotId, 4)));

        assertThat(lot.getCurrentQuantity()).isEqualTo(4);
        assertThat(lot.getStatus()).isEqualTo(LotStatus.ACTIVE);
        verify(productLotRepository).save(lot);
    }

    @Test
    void releaseAllocationsIgnoresLotsThatNoLongerExist() {
        UUID missingLotId = UUID.randomUUID();
        when(productLotRepository.findById(missingLotId)).thenReturn(Optional.empty());

        service.releaseAllocations(List.of(new LotAllocation(missingLotId, 4)));

        verify(productLotRepository, never()).save(any());
    }

    @Test
    void savesEachDecrementedLotExactlyOnce() {
        ProductLot lot = lot(LocalDate.now().plusMonths(1), 10);
        when(productLotRepository.findAllocatableLotsForUpdate(productId, warehouseId)).thenReturn(List.of(lot));

        service.allocateStock(warehouseId, productId, 4);

        ArgumentCaptor<ProductLot> captor = ArgumentCaptor.forClass(ProductLot.class);
        verify(productLotRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCurrentQuantity()).isEqualTo(6);
    }
}
