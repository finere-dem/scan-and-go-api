package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.*;
import com.finere.scan_and_go_api.domain.enums.OrgType;
import com.finere.scan_and_go_api.domain.enums.PaymentMode;
import com.finere.scan_and_go_api.domain.enums.TargetOrgType;
import com.finere.scan_and_go_api.dto.order.LotAllocation;
import com.finere.scan_and_go_api.dto.order.OrderCreateRequest;
import com.finere.scan_and_go_api.dto.order.OrderItemRequest;
import com.finere.scan_and_go_api.dto.order.OrderResponse;
import com.finere.scan_and_go_api.exception.CreditLimitExceededException;
import com.finere.scan_and_go_api.exception.InsufficientStockException;
import com.finere.scan_and_go_api.repository.*;
import com.finere.scan_and_go_api.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * These tests exist to lock in a real bug that was found and fixed: FEFO stock allocation
 * commits independently (REQUIRES_NEW, see InventoryAllocationService), so if credit/MOQ
 * validation ran AFTER allocation, a rejected order still left stock permanently consumed.
 * The fix reorders validation before allocation - these tests fail loudly if that ordering
 * ever regresses.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PricingPolicyRepository pricingPolicyRepository;
    @Mock private ProductLotRepository productLotRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InventoryAllocationService inventoryAllocationService;
    @Mock private CreditManagementService creditManagementService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private OrderService orderService;

    private final UUID buyerOrgId = UUID.randomUUID();
    private final UUID sellerOrgId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();
    private final UUID lotId = UUID.randomUUID();

    private Organization buyerOrg;
    private Organization sellerOrg;
    private Product product;
    private PricingPolicy pricing;

    @BeforeEach
    void setUp() {
        buyerOrg = new Organization();
        buyerOrg.setId(buyerOrgId);
        buyerOrg.setOrgType(OrgType.WHOLESALER);

        sellerOrg = new Organization();
        sellerOrg.setId(sellerOrgId);
        sellerOrg.setOrgType(OrgType.IMPORTER);

        product = new Product();
        product.setId(productId);

        pricing = new PricingPolicy();
        pricing.setTargetOrgType(TargetOrgType.WHOLESALER);
        pricing.setUnitPrice(new BigDecimal("1000"));
        pricing.setMinOrderQty(1);

        lenient().when(organizationRepository.findById(buyerOrgId)).thenReturn(Optional.of(buyerOrg));
        lenient().when(organizationRepository.findById(sellerOrgId)).thenReturn(Optional.of(sellerOrg));
        lenient().when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        lenient().when(pricingPolicyRepository.findBySellerOrgIdAndProductIdAndTargetOrgType(
                        sellerOrgId, productId, TargetOrgType.WHOLESALER))
                .thenReturn(Optional.of(pricing));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productLotRepository.getReferenceById(any())).thenAnswer(inv -> {
            ProductLot lot = new ProductLot();
            lot.setId(inv.getArgument(0));
            return lot;
        });
    }

    private OrderCreateRequest request(PaymentMode paymentMode, int quantity) {
        return new OrderCreateRequest(
                buyerOrgId, sellerOrgId, paymentMode, null, null, null,
                List.of(new OrderItemRequest(productId, warehouseId, quantity)));
    }

    @Test
    void cashOrderAllocatesStockAndComputesTotal() {
        pricing.setMinOrderQty(1);
        when(inventoryAllocationService.allocateStock(warehouseId, productId, 5))
                .thenReturn(List.of(new LotAllocation(lotId, 5)));

        OrderResponse response = orderService.createOrder(request(PaymentMode.CASH, 5));

        assertThat(response.totalAmount()).isEqualByComparingTo("5000");
        assertThat(response.items()).hasSize(1);
        verify(creditManagementService, never()).validateOrderAgainstCredit(any(), any(), any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void creditOrderChecksCreditBeforeAllocatingStock() {
        when(inventoryAllocationService.allocateStock(warehouseId, productId, 5))
                .thenReturn(List.of(new LotAllocation(lotId, 5)));

        orderService.createOrder(request(PaymentMode.CREDIT_30, 5));

        var inOrder = inOrder(creditManagementService, inventoryAllocationService);
        inOrder.verify(creditManagementService).validateOrderAgainstCredit(sellerOrgId, buyerOrgId, new BigDecimal("5000"));
        inOrder.verify(inventoryAllocationService).allocateStock(warehouseId, productId, 5);
    }

    @Test
    void creditRejectionNeverTouchesStockAllocation() {
        doThrow(new CreditLimitExceededException("over limit"))
                .when(creditManagementService).validateOrderAgainstCredit(any(), any(), any());

        assertThatThrownBy(() -> orderService.createOrder(request(PaymentMode.CREDIT_30, 5)))
                .isInstanceOf(CreditLimitExceededException.class);

        verifyNoInteractions(inventoryAllocationService);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void creditOrderIncreasesBalanceAndCreatesInvoiceOnlyAfterSuccess() {
        when(inventoryAllocationService.allocateStock(warehouseId, productId, 5))
                .thenReturn(List.of(new LotAllocation(lotId, 5)));

        orderService.createOrder(request(PaymentMode.CREDIT_30, 5));

        verify(creditManagementService).increaseBalance(sellerOrgId, buyerOrgId, new BigDecimal("5000"));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void belowMinimumOrderQuantityIsRejectedWithoutTouchingStock() {
        pricing.setMinOrderQty(10);

        assertThatThrownBy(() -> orderService.createOrder(request(PaymentMode.CASH, 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum order quantity");

        verifyNoInteractions(inventoryAllocationService);
    }

    @Test
    void unknownPricingPolicyIsRejectedBeforeAnyAllocation() {
        when(pricingPolicyRepository.findBySellerOrgIdAndProductIdAndTargetOrgType(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request(PaymentMode.CASH, 5)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(inventoryAllocationService);
    }

    @Test
    void insufficientStockOnASingleItemOrderPropagatesAndSkipsRelease() {
        when(inventoryAllocationService.allocateStock(warehouseId, productId, 5))
                .thenThrow(new InsufficientStockException("not enough"));

        assertThatThrownBy(() -> orderService.createOrder(request(PaymentMode.CASH, 5)))
                .isInstanceOf(InsufficientStockException.class);

        // Nothing was allocated yet on this - the only - item, so there's nothing to release.
        verify(inventoryAllocationService, never()).releaseAllocations(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void secondItemFailureReleasesTheFirstItemsAllocation() {
        UUID secondProductId = UUID.randomUUID();
        Product secondProduct = new Product();
        secondProduct.setId(secondProductId);
        PricingPolicy secondPricing = new PricingPolicy();
        secondPricing.setTargetOrgType(TargetOrgType.WHOLESALER);
        secondPricing.setUnitPrice(new BigDecimal("500"));
        secondPricing.setMinOrderQty(1);

        when(productRepository.findById(secondProductId)).thenReturn(Optional.of(secondProduct));
        when(pricingPolicyRepository.findBySellerOrgIdAndProductIdAndTargetOrgType(
                        sellerOrgId, secondProductId, TargetOrgType.WHOLESALER))
                .thenReturn(Optional.of(secondPricing));

        LotAllocation firstAllocation = new LotAllocation(lotId, 5);
        when(inventoryAllocationService.allocateStock(warehouseId, productId, 5))
                .thenReturn(List.of(firstAllocation));
        when(inventoryAllocationService.allocateStock(warehouseId, secondProductId, 2))
                .thenThrow(new InsufficientStockException("not enough for second item"));

        OrderCreateRequest request = new OrderCreateRequest(
                buyerOrgId, sellerOrgId, PaymentMode.CASH, null, null, null,
                List.of(
                        new OrderItemRequest(productId, warehouseId, 5),
                        new OrderItemRequest(secondProductId, warehouseId, 2)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryAllocationService).releaseAllocations(eq(List.of(firstAllocation)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void repeatedClientSyncIdReturnsExistingOrderWithoutReValidating() {
        UUID clientSyncId = UUID.randomUUID();
        Order existing = new Order();
        existing.setId(UUID.randomUUID());
        existing.setOrderNumber("ORD-EXISTING");
        existing.setBuyerOrg(buyerOrg);
        existing.setSellerOrg(sellerOrg);
        existing.setPaymentMode(PaymentMode.CASH);
        existing.setClientSyncId(clientSyncId);
        when(orderRepository.findByClientSyncId(clientSyncId)).thenReturn(Optional.of(existing));

        OrderCreateRequest request = new OrderCreateRequest(
                buyerOrgId, sellerOrgId, PaymentMode.CASH, clientSyncId, null, null,
                List.of(new OrderItemRequest(productId, warehouseId, 5)));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.orderNumber()).isEqualTo("ORD-EXISTING");
        verifyNoInteractions(inventoryAllocationService, pricingPolicyRepository, productRepository);
    }

    @Test
    void retailerBuyerResolvesToRetailerPricingTier() {
        buyerOrg.setOrgType(OrgType.RETAILER);
        pricing.setTargetOrgType(TargetOrgType.RETAILER);
        when(pricingPolicyRepository.findBySellerOrgIdAndProductIdAndTargetOrgType(
                        sellerOrgId, productId, TargetOrgType.RETAILER))
                .thenReturn(Optional.of(pricing));
        when(inventoryAllocationService.allocateStock(warehouseId, productId, 1))
                .thenReturn(List.of(new LotAllocation(lotId, 1)));

        orderService.createOrder(request(PaymentMode.CASH, 1));

        verify(pricingPolicyRepository).findBySellerOrgIdAndProductIdAndTargetOrgType(
                sellerOrgId, productId, TargetOrgType.RETAILER);
    }

    @Test
    void importerBuyerCannotPlaceAnOrder() {
        buyerOrg.setOrgType(OrgType.IMPORTER);

        assertThatThrownBy(() -> orderService.createOrder(request(PaymentMode.CASH, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesCallerOwnsTheBuyerOrgBeforeAnythingElse() {
        doThrow(new org.springframework.security.access.AccessDeniedException("not your org"))
                .when(currentUserService).requireSameOrgOrSuperAdmin(buyerOrgId);

        assertThatThrownBy(() -> orderService.createOrder(request(PaymentMode.CASH, 1)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verifyNoInteractions(organizationRepository, productRepository, inventoryAllocationService);
    }
}
