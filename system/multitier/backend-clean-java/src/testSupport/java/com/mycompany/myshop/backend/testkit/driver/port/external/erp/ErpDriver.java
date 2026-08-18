package com.mycompany.myshop.backend.testkit.driver.port.external.erp;

public interface ErpDriver {

    void goToErp();

    void returnsProduct(String sku, String price);

    void returnsNoProduct(String sku);

    void returnsPromotion(boolean active, String discount);

    void failsForProduct(String sku, int status, String body);

    void failsForPromotion(int status, String body);
}
