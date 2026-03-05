package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.model.StoreProduct;
import com.finere.scan_and_go_api.model.Store;
import com.finere.scan_and_go_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StoreProductRepository extends JpaRepository<StoreProduct, Long> {
    Optional<StoreProduct> findByStoreAndProduct(Store store, Product product);
    Optional<StoreProduct> findByStoreIdAndProductBarcode(Long storeId, String barcode);
}
