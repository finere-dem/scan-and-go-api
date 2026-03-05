package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.dto.CheckoutRequest;
import com.finere.scan_and_go_api.dto.ScanResponse;
import com.finere.scan_and_go_api.model.Order;
import com.finere.scan_and_go_api.model.Store;
import com.finere.scan_and_go_api.model.StoreProduct;
import com.finere.scan_and_go_api.repository.OrderRepository;
import com.finere.scan_and_go_api.repository.StoreProductRepository;
import com.finere.scan_and_go_api.repository.StoreRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopService {

    @Autowired
    private StoreProductRepository storeProductRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    public ScanResponse scanProduct(String barcode, Long storeId) {
        StoreProduct storeProduct = storeProductRepository.findByStoreIdAndProductBarcode(storeId, barcode)
                .orElseThrow(() -> new RuntimeException("Product not found in this store"));

        return new ScanResponse(
                storeProduct.getProduct().getName(),
                storeProduct.getPrice(),
                storeProduct.getStore().getName(),
                storeProduct.getProduct().getId()
        );
    }

    @Transactional
    public Long checkout(CheckoutRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        double totalAmount = 0.0;

        for (Long productId : request.getProductIds()) {
            StoreProduct storeProduct = storeProductRepository.findByStoreAndProduct(store, productRepository.findById(productId).orElseThrow())
                     .orElseThrow(() -> new RuntimeException("Product not found in store during checkout"));
            totalAmount += storeProduct.getPrice();
        }

        Order order = new Order();
        order.setStore(store);
        order.setTotalAmount(totalAmount);
        order.setStatus("COMPLETED");
        
        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }
}
