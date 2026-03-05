package com.finere.scan_and_go_api.dto;

import java.util.List;

public class CheckoutRequest {
    private Long storeId;
    private List<Long> productIds;

    public CheckoutRequest() {
    }

    public CheckoutRequest(Long storeId, List<Long> productIds) {
        this.storeId = storeId;
        this.productIds = productIds;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }
}
