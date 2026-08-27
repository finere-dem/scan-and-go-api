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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lets a retailer freely reprice a product for their own storefront, independent of what they paid. */
@Service
@RequiredArgsConstructor
public class LocalRetailPriceService {

    private final LocalRetailPriceRepository localRetailPriceRepository;
    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public LocalRetailPriceResponse upsert(LocalRetailPriceRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(request.retailerOrgId());

        LocalRetailPrice entity = localRetailPriceRepository
                .findByRetailerOrgIdAndProductId(request.retailerOrgId(), request.productId())
                .orElseGet(() -> {
                    Organization retailerOrg = organizationRepository.findById(request.retailerOrgId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Unknown retailer organization: " + request.retailerOrgId()));
                    Product product = productRepository.findById(request.productId())
                            .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + request.productId()));

                    LocalRetailPrice created = new LocalRetailPrice();
                    created.setRetailerOrg(retailerOrg);
                    created.setProduct(product);
                    return created;
                });

        entity.setConsumerPrice(request.consumerPrice());
        if (request.currency() != null && !request.currency().isBlank()) {
            entity.setCurrency(request.currency());
        }
        entity.setUpdatedAt(Instant.now());

        return toResponse(localRetailPriceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public LocalRetailPriceResponse getByRetailerAndProduct(UUID retailerOrgId, UUID productId) {
        return localRetailPriceRepository.findByRetailerOrgIdAndProductId(retailerOrgId, productId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No local retail price for retailer " + retailerOrgId + " and product " + productId));
    }

    @Transactional(readOnly = true)
    public List<LocalRetailPriceResponse> listByRetailer(UUID retailerOrgId) {
        currentUserService.requireSameOrgOrSuperAdmin(retailerOrgId);
        return localRetailPriceRepository.findByRetailerOrgId(retailerOrgId).stream().map(this::toResponse).toList();
    }

    private LocalRetailPriceResponse toResponse(LocalRetailPrice entity) {
        return new LocalRetailPriceResponse(
                entity.getId(),
                entity.getRetailerOrg().getId(),
                entity.getProduct().getId(),
                entity.getConsumerPrice(),
                entity.getCurrency(),
                entity.getUpdatedAt());
    }
}
