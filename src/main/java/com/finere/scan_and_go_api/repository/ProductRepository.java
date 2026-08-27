package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByEan13(String ean13);
    Optional<Product> findByImporterIdAndSku(UUID importerId, String sku);
    List<Product> findByImporterId(UUID importerId);
}
