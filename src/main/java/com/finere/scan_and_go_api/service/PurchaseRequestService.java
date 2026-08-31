package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.*;
import com.finere.scan_and_go_api.domain.enums.PurchaseRequestStatus;
import com.finere.scan_and_go_api.dto.order.OrderCreateRequest;
import com.finere.scan_and_go_api.dto.order.OrderItemRequest;
import com.finere.scan_and_go_api.dto.order.OrderResponse;
import com.finere.scan_and_go_api.dto.purchase.*;
import com.finere.scan_and_go_api.repository.*;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A PURCHASE_STAFF account cannot place a real order directly - it submits a request that sits
 * PENDING until the org's owner (WHOLESALER_ADMIN/RETAILER_ADMIN) approves or rejects it. Approval
 * hands off to the exact same {@link OrderService#createOrder} path a direct order already uses
 * (FEFO allocation, credit validation, invoicing) so the two paths can never drift apart; a
 * rejection never touches stock at all.
 */
@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @Transactional
    public PurchaseRequestResponse create(PurchaseRequestCreateRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(request.buyerOrgId());

        Organization buyerOrg = organizationRepository.findById(request.buyerOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown buyer organization: " + request.buyerOrgId()));
        Organization sellerOrg = organizationRepository.findById(request.sellerOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown seller organization: " + request.sellerOrgId()));
        User requestedBy = userRepository.getReferenceById(currentUserService.requireUserId());

        if (request.receivingWarehouseId() != null && request.receivingBoutiqueId() != null) {
            throw new IllegalArgumentException("A request can only receive stock into one location, not both");
        }

        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setBuyerOrg(buyerOrg);
        purchaseRequest.setSellerOrg(sellerOrg);
        purchaseRequest.setRequestedBy(requestedBy);
        purchaseRequest.setPaymentMode(request.paymentMode());
        if (request.receivingWarehouseId() != null) {
            purchaseRequest.setReceivingWarehouse(warehouseRepository.getReferenceById(request.receivingWarehouseId()));
        }
        if (request.receivingBoutiqueId() != null) {
            purchaseRequest.setReceivingBoutique(boutiqueRepository.getReferenceById(request.receivingBoutiqueId()));
        }

        for (PurchaseRequestItemInput itemInput : request.items()) {
            Product product = productRepository.findById(itemInput.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + itemInput.productId()));
            Warehouse warehouse = warehouseRepository.findById(itemInput.warehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown warehouse: " + itemInput.warehouseId()));

            PurchaseRequestItem item = new PurchaseRequestItem();
            item.setPurchaseRequest(purchaseRequest);
            item.setProduct(product);
            item.setWarehouse(warehouse);
            item.setQuantity(itemInput.quantity());
            purchaseRequest.getItems().add(item);
        }

        return toResponse(purchaseRequestRepository.save(purchaseRequest));
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> listByBuyer(UUID buyerOrgId, PurchaseRequestStatus status) {
        currentUserService.requireSameOrgOrSuperAdmin(buyerOrgId);
        List<PurchaseRequest> requests = status != null
                ? purchaseRequestRepository.findByBuyerOrgIdAndStatus(buyerOrgId, status)
                : purchaseRequestRepository.findByBuyerOrgId(buyerOrgId);
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse approve(UUID requestId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown purchase request: " + requestId));
        currentUserService.requireSameOrgOrSuperAdmin(purchaseRequest.getBuyerOrg().getId());
        requirePending(purchaseRequest);

        OrderCreateRequest orderRequest = new OrderCreateRequest(
                purchaseRequest.getBuyerOrg().getId(),
                purchaseRequest.getSellerOrg().getId(),
                purchaseRequest.getPaymentMode(),
                null,
                purchaseRequest.getReceivingWarehouse() != null ? purchaseRequest.getReceivingWarehouse().getId() : null,
                purchaseRequest.getReceivingBoutique() != null ? purchaseRequest.getReceivingBoutique().getId() : null,
                purchaseRequest.getItems().stream()
                        .map(item -> new OrderItemRequest(item.getProduct().getId(), item.getWarehouse().getId(), item.getQuantity()))
                        .toList());

        OrderResponse order = orderService.createOrder(orderRequest);

        purchaseRequest.setStatus(PurchaseRequestStatus.APPROVED);
        purchaseRequest.setDecidedBy(userRepository.getReferenceById(currentUserService.requireUserId()));
        purchaseRequest.setDecidedAt(Instant.now());
        purchaseRequest.setResultingOrderId(order.id());
        purchaseRequestRepository.save(purchaseRequest);

        return order;
    }

    @Transactional
    public PurchaseRequestResponse reject(UUID requestId, PurchaseRequestRejectRequest request) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown purchase request: " + requestId));
        currentUserService.requireSameOrgOrSuperAdmin(purchaseRequest.getBuyerOrg().getId());
        requirePending(purchaseRequest);

        purchaseRequest.setStatus(PurchaseRequestStatus.REJECTED);
        purchaseRequest.setRejectionReason(request.reason());
        purchaseRequest.setDecidedBy(userRepository.getReferenceById(currentUserService.requireUserId()));
        purchaseRequest.setDecidedAt(Instant.now());

        return toResponse(purchaseRequestRepository.save(purchaseRequest));
    }

    private void requirePending(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new AccessDeniedException("Purchase request " + purchaseRequest.getId() + " has already been " + purchaseRequest.getStatus());
        }
    }

    private PurchaseRequestResponse toResponse(PurchaseRequest purchaseRequest) {
        List<PurchaseRequestItemResponse> items = purchaseRequest.getItems().stream()
                .map(item -> new PurchaseRequestItemResponse(item.getProduct().getId(), item.getWarehouse().getId(), item.getQuantity()))
                .toList();

        User requestedBy = purchaseRequest.getRequestedBy();
        String requestedByName = ((requestedBy.getFirstName() != null ? requestedBy.getFirstName() : "") + " "
                + (requestedBy.getLastName() != null ? requestedBy.getLastName() : "")).trim();

        return new PurchaseRequestResponse(
                purchaseRequest.getId(),
                purchaseRequest.getBuyerOrg().getId(),
                purchaseRequest.getSellerOrg().getId(),
                requestedBy.getId(),
                requestedByName.isEmpty() ? requestedBy.getPhone() : requestedByName,
                purchaseRequest.getPaymentMode(),
                purchaseRequest.getReceivingWarehouse() != null ? purchaseRequest.getReceivingWarehouse().getId() : null,
                purchaseRequest.getReceivingBoutique() != null ? purchaseRequest.getReceivingBoutique().getId() : null,
                purchaseRequest.getStatus(),
                purchaseRequest.getRejectionReason(),
                purchaseRequest.getDecidedBy() != null ? purchaseRequest.getDecidedBy().getId() : null,
                purchaseRequest.getDecidedAt(),
                purchaseRequest.getResultingOrderId(),
                purchaseRequest.getCreatedAt(),
                items);
    }
}
