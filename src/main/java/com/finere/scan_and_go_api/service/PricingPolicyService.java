package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.PricingPolicy;
import com.finere.scan_and_go_api.domain.entity.Product;
import com.finere.scan_and_go_api.dto.pricing.PricingPolicyRequest;
import com.finere.scan_and_go_api.dto.pricing.PricingPolicyResponse;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.repository.PricingPolicyRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingPolicyService {

    private final PricingPolicyRepository pricingPolicyRepository;
    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PricingPolicyResponse create(PricingPolicyRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(request.sellerOrgId());

        Organization sellerOrg = organizationRepository.findById(request.sellerOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown seller organization: " + request.sellerOrgId()));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + request.productId()));

        PricingPolicy policy = new PricingPolicy();
        policy.setSellerOrg(sellerOrg);
        policy.setProduct(product);
        policy.setTargetOrgType(request.targetOrgType());
        policy.setUnitPrice(request.unitPrice());
        policy.setMinOrderQty(request.minOrderQty());
        if (request.currency() != null && !request.currency().isBlank()) {
            policy.setCurrency(request.currency());
        }

        return toResponse(pricingPolicyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public List<PricingPolicyResponse> listBySeller(UUID sellerOrgId) {
        return pricingPolicyRepository.findBySellerOrgId(sellerOrgId).stream().map(this::toResponse).toList();
    }

    private PricingPolicyResponse toResponse(PricingPolicy policy) {
        return new PricingPolicyResponse(
                policy.getId(),
                policy.getSellerOrg().getId(),
                policy.getProduct().getId(),
                policy.getTargetOrgType(),
                policy.getUnitPrice(),
                policy.getMinOrderQty(),
                policy.getCurrency());
    }
}
