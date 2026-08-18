package com.mycompany.myshop.backend.usecases.order;

public class RecallSkuResponse {

    private String sku;
    private int cancelledCount;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }
}
