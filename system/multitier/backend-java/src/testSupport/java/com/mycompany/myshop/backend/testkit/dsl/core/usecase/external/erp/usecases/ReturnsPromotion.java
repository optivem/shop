package com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.base.BaseErpUseCase;

public class ReturnsPromotion extends BaseErpUseCase {

    private boolean active;
    private String discount;

    public ReturnsPromotion(ErpStubDriver driver) {
        super(driver);
    }

    public ReturnsPromotion active(boolean active) {
        this.active = active;
        return this;
    }

    public ReturnsPromotion discount(String discount) {
        this.discount = discount;
        return this;
    }

    @Override
    public void execute() {
        driver.stubPromotion(active, discount);
    }
}
