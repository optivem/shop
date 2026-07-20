package com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.base.BaseErpUseCase;

/** The ERP answers {@code 404} for this SKU — an unknown product, programmed as deliberately as a known one. */
public class ReturnsNoProduct extends BaseErpUseCase {

    private String sku;

    public ReturnsNoProduct(ErpStubDriver driver) {
        super(driver);
    }

    public ReturnsNoProduct sku(String sku) {
        this.sku = sku;
        return this;
    }

    @Override
    public void execute() {
        driver.stubProductMissing(sku);
    }
}
