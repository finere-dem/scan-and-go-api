package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.Product;
import com.finere.scan_and_go_api.dto.product.ProductRequest;
import com.finere.scan_and_go_api.dto.product.ProductResponse;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        currentUserService.requireSameOrgOrSuperAdmin(request.importerId());

        Organization importer = organizationRepository.findById(request.importerId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown importer: " + request.importerId()));

        Product product = new Product();
        product.setImporter(importer);
        product.setSku(request.sku());
        product.setEan13(request.ean13());
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setCategory(request.category());
        product.setPackagingType(request.packagingType());
        product.setUnitsPerBox(request.unitsPerBox());

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getByEan13(String ean13) {
        return productRepository.findByEan13(ean13)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("No product with EAN-13: " + ean13));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listByImporter(UUID importerId) {
        return productRepository.findByImporterId(importerId).stream().map(this::toResponse).toList();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getImporter().getId(),
                product.getSku(),
                product.getEan13(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getPackagingType(),
                product.getUnitsPerBox());
    }
}
