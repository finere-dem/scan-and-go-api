package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.*;
import com.finere.scan_and_go_api.domain.enums.OrderStatus;
import com.finere.scan_and_go_api.domain.enums.OrgType;
import com.finere.scan_and_go_api.domain.enums.TargetOrgType;
import com.finere.scan_and_go_api.dto.order.*;
import com.finere.scan_and_go_api.repository.*;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates B2B order submission: pricing lookup (with MOQ enforcement), FEFO stock
 * allocation, credit validation for CREDIT payment modes, and invoice creation.
 *
 * <p>Supplier isolation (one seller per cart) and the client-side MOQ gate are enforced first
 * on the Flutter cart itself; this service re-validates both server-side since the mobile app
 * can be offline and stale by the time the order actually reaches the API.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final ProductLotRepository productLotRepository;
    private final InvoiceRepository invoiceRepository;
    private final InventoryAllocationService inventoryAllocationService;
    private final CreditManagementService creditManagementService;
    private final CurrentUserService currentUserService;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        // A buyer places orders on behalf of their own org only - not arbitrary buyerOrgId values.
        currentUserService.requireSameOrgOrSuperAdmin(request.buyerOrgId());

        if (request.clientSyncId() != null) {
            var existing = orderRepository.findByClientSyncId(request.clientSyncId());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Organization buyerOrg = organizationRepository.findById(request.buyerOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown buyer organization: " + request.buyerOrgId()));
        Organization sellerOrg = organizationRepository.findById(request.sellerOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown seller organization: " + request.sellerOrgId()));

        TargetOrgType targetOrgType = resolveTargetOrgType(buyerOrg.getOrgType());

        // Pass 1: resolve pricing, enforce MOQ, and compute the order total WITHOUT touching
        // stock yet. FEFO allocation runs in its own REQUIRES_NEW transaction (see
        // InventoryAllocationService) and commits independently the moment it succeeds, so any
        // validation that can still fail the order (credit limit, MOQ, unknown pricing) must run
        // to completion before a single unit is decremented — otherwise a later failure leaves
        // stock permanently and silently consumed for an order that was never created.
        List<ResolvedItem> resolvedItems = request.items().stream()
                .map(itemRequest -> resolveItem(sellerOrg.getId(), targetOrgType, itemRequest))
                .toList();

        BigDecimal total = resolvedItems.stream()
                .map(ResolvedItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.paymentMode().isCredit()) {
            creditManagementService.validateOrderAgainstCredit(sellerOrg.getId(), buyerOrg.getId(), total);
        }

        // Pass 2: all validations passed - now allocate stock and build the order.
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setBuyerOrg(buyerOrg);
        order.setSellerOrg(sellerOrg);
        order.setPaymentMode(request.paymentMode());
        order.setClientSyncId(request.clientSyncId());
        order.setTotalAmount(total);
        order.setOrderStatus(OrderStatus.CONFIRMED);

        List<LotAllocation> allAllocationsSoFar = new ArrayList<>();
        try {
            for (ResolvedItem resolved : resolvedItems) {
                List<LotAllocation> allocations = inventoryAllocationService.allocateStock(
                        resolved.warehouseId(), resolved.product().getId(), resolved.quantity());
                allAllocationsSoFar.addAll(allocations);

                for (LotAllocation allocation : allocations) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(resolved.product());
                    orderItem.setLot(productLotRepository.getReferenceById(allocation.lotId()));
                    orderItem.setQuantity(allocation.quantity());
                    orderItem.setUnitPrice(resolved.unitPrice());
                    orderItem.setSubtotal(resolved.unitPrice().multiply(BigDecimal.valueOf(allocation.quantity())));
                    order.getItems().add(orderItem);
                }
            }
        } catch (RuntimeException e) {
            if (!allAllocationsSoFar.isEmpty()) {
                inventoryAllocationService.releaseAllocations(allAllocationsSoFar);
            }
            throw e;
        }

        Order saved = orderRepository.save(order);

        if (request.paymentMode().isCredit()) {
            creditManagementService.increaseBalance(sellerOrg.getId(), buyerOrg.getId(), total);
            createInvoice(saved, request.paymentMode().termDays());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByBuyer(UUID buyerOrgId) {
        currentUserService.requireSameOrgOrSuperAdmin(buyerOrgId);
        return orderRepository.findByBuyerOrgId(buyerOrgId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listBySeller(UUID sellerOrgId) {
        currentUserService.requireSameOrgOrSuperAdmin(sellerOrgId);
        return orderRepository.findBySellerOrgId(sellerOrgId).stream().map(this::toResponse).toList();
    }

    private ResolvedItem resolveItem(UUID sellerOrgId, TargetOrgType targetOrgType, OrderItemRequest itemRequest) {
        Product product = productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + itemRequest.productId()));

        PricingPolicy pricing = pricingPolicyRepository
                .findBySellerOrgIdAndProductIdAndTargetOrgType(sellerOrgId, product.getId(), targetOrgType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pricing policy from seller " + sellerOrgId + " for product " + product.getId()
                                + " targeting " + targetOrgType));

        if (itemRequest.quantity() < pricing.getMinOrderQty()) {
            throw new IllegalArgumentException(
                    "Quantity " + itemRequest.quantity() + " is below the minimum order quantity ("
                            + pricing.getMinOrderQty() + ") for product " + product.getId());
        }

        BigDecimal subtotal = pricing.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
        return new ResolvedItem(product, itemRequest.warehouseId(), itemRequest.quantity(), pricing.getUnitPrice(), subtotal);
    }

    private record ResolvedItem(Product product, UUID warehouseId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    private TargetOrgType resolveTargetOrgType(OrgType buyerOrgType) {
        return switch (buyerOrgType) {
            case WHOLESALER -> TargetOrgType.WHOLESALER;
            case RETAILER -> TargetOrgType.RETAILER;
            default -> throw new IllegalArgumentException(
                    "Organization type " + buyerOrgType + " cannot place B2B orders");
        };
    }

    private void createInvoice(Order order, int termDays) {
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber("INV-" + order.getOrderNumber());
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(termDays));
        invoice.setAmountDue(order.getTotalAmount());
        invoiceRepository.save(invoice);
    }

    private String generateOrderNumber() {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + LocalDate.now() + "-" + randomSuffix;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getLot() != null ? item.getLot().getId() : null,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBuyerOrg().getId(),
                order.getSellerOrg().getId(),
                order.getTotalAmount(),
                order.getPaymentMode(),
                order.getOrderStatus(),
                order.getClientSyncId(),
                order.getCreatedAt(),
                items);
    }
}
