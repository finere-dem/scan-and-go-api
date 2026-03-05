package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByBarcode(String barcode);
}
