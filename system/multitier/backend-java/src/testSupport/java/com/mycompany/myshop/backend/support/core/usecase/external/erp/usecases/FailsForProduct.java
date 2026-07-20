package com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.base.BaseErpUseCase;

/**
 * The ERP is broken for this SKU — a {@code 5xx} rather than an answer. Distinct from
 * {@link ReturnsNoProduct}: a 404 is the ERP correctly reporting absence, this is the ERP failing to
 * report anything.
 */
public class FailsForProduct extends BaseErpUseCase {

    private String sku;
    private int status;
    private String body;

    public FailsForProduct(ErpStubDriver driver) {
        super(driver);
    }

    public FailsForProduct sku(String sku) {
        this.sku = sku;
        return this;
    }

    public FailsForProduct status(int status) {
        this.status = status;
        return this;
    }

    public FailsForProduct body(String body) {
        this.body = body;
        return this;
    }

    @Override
    public void execute() {
        driver.stubProductError(sku, status, body);
    }
}
