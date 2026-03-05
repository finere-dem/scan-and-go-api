package com.finere.scan_and_go_api;

import com.finere.scan_and_go_api.model.Product;
import com.finere.scan_and_go_api.model.Store;
import com.finere.scan_and_go_api.model.StoreProduct;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.repository.StoreProductRepository;
import com.finere.scan_and_go_api.repository.StoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(StoreRepository storeRepository, 
                                   ProductRepository productRepository, 
                                   StoreProductRepository storeProductRepository) {
        return args -> {
            if (storeRepository.count() == 0) {
                // Create Stores
                Store leclerc = new Store(null, "Leclerc Bondy", "Ave. Gallieni");
                Store carrefour = new Store(null, "Carrefour Paris", "Rue de Rivoli");
                
                storeRepository.save(leclerc);
                storeRepository.save(carrefour);

                // Create Products
                Product coca = new Product(null, "5449000000996", "Coca Cola", "Soda 33cl");
                Product nutella = new Product(null, "3017620422003", "Nutella", "Pate a tartiner");

                productRepository.save(coca);
                productRepository.save(nutella);

                // Link Products to Stores with Prices
                // Leclerc: Coca 1.20, Nutella 5.00
                storeProductRepository.save(new StoreProduct(null, leclerc, coca, 1.20));
                storeProductRepository.save(new StoreProduct(null, leclerc, nutella, 5.00));
                
                // Carrefour: Coca 1.50, Nutella 5.50
                storeProductRepository.save(new StoreProduct(null, carrefour, coca, 1.50));
                storeProductRepository.save(new StoreProduct(null, carrefour, nutella, 5.50));
                
                System.out.println("Data Seeding Completed!");
            }
        };
    }
}
