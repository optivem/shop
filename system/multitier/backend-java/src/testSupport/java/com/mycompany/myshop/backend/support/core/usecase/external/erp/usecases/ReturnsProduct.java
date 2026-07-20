package com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.base.BaseErpUseCase;

public class ReturnsProduct extends BaseErpUseCase {

    private String sku;
    private String unitPrice;

    public ReturnsProduct(ErpStubDriver driver) {
        super(driver);
    }

    public ReturnsProduct sku(String sku) {
        this.sku = sku;
        return this;
    }

    public ReturnsProduct unitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    @Override
    public void execute() {
        driver.stubProduct(sku, unitPrice);
    }
}
