package com.finere.scan_and_go_api.dto;

public class ScanResponse {
    private String productName;
    private Double price;
    private String storeName;
    private Long productId;

    public ScanResponse() {
    }

    public ScanResponse(String productName, Double price, String storeName, Long productId) {
        this.productName = productName;
        this.price = price;
        this.storeName = storeName;
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
