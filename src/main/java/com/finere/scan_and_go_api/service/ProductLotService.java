package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Boutique;
import com.finere.scan_and_go_api.domain.entity.Product;
import com.finere.scan_and_go_api.domain.entity.ProductLot;
import com.finere.scan_and_go_api.domain.entity.Warehouse;
import com.finere.scan_and_go_api.dto.product.ProductLotRequest;
import com.finere.scan_and_go_api.dto.product.ProductLotResponse;
import com.finere.scan_and_go_api.repository.BoutiqueRepository;
import com.finere.scan_and_go_api.repository.ProductLotRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.repository.WarehouseRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductLotService {

    private final ProductLotRepository productLotRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public ProductLotResponse create(ProductLotRequest request) {
        if (!request.expDate().isAfter(request.mfgDate())) {
            throw new IllegalArgumentException("Expiry date must be after manufacturing date");
        }
        if ((request.warehouseId() == null) == (request.boutiqueId() == null)) {
            throw new IllegalArgumentException("Exactly one of warehouseId or boutiqueId must be provided");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + request.productId()));

        ProductLot lot = new ProductLot();
        lot.setProduct(product);

        if (request.warehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown warehouse: " + request.warehouseId()));
            // Scoped to the warehouse's owning org: whoever physically operates the depot is who
            // may record a receiving lot into it, regardless of which importer catalogued the product.
            currentUserService.requireSameOrgOrSuperAdmin(warehouse.getOrganization().getId());
            lot.setWarehouse(warehouse);
        } else {
            Boutique boutique = boutiqueRepository.findById(request.boutiqueId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown boutique: " + request.boutiqueId()));
            currentUserService.requireSameOrgOrSuperAdmin(boutique.getOrganization().getId());
            lot.setBoutique(boutique);
        }

        lot.setLotNumber(request.lotNumber());
        lot.setMfgDate(request.mfgDate());
        lot.setExpDate(request.expDate());
        lot.setInitialQuantity(request.initialQuantity());
        lot.setCurrentQuantity(request.initialQuantity());
        lot.setUnitCost(request.unitCost());

        return toResponse(productLotRepository.save(lot));
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> listByWarehouse(UUID warehouseId) {
        return productLotRepository.findByWarehouseId(warehouseId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> listByBoutique(UUID boutiqueId) {
        return productLotRepository.findByBoutiqueId(boutiqueId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> listByProduct(UUID productId) {
        return productLotRepository.findByProductId(productId).stream().map(this::toResponse).toList();
    }

    private ProductLotResponse toResponse(ProductLot lot) {
        // Purchase cost is never shown to a sales profile, even for stock they can otherwise see -
        // confidentiality of what the org paid is separate from visibility of quantities on hand.
        boolean hideCost = currentUserService.hasRole("ROLE_SALES_STAFF");
        return new ProductLotResponse(
                lot.getId(),
                lot.getProduct().getId(),
                lot.getWarehouse() != null ? lot.getWarehouse().getId() : null,
                lot.getBoutique() != null ? lot.getBoutique().getId() : null,
                lot.getLotNumber(),
                lot.getMfgDate(),
                lot.getExpDate(),
                lot.getInitialQuantity(),
                lot.getCurrentQuantity(),
                hideCost ? null : lot.getUnitCost(),
                lot.getStatus());
    }
}
