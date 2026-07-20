package com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.base.BaseErpUseCase;

/** The ERP is broken for the promotion lookup — a {@code 5xx} rather than an answer. */
public class FailsForPromotion extends BaseErpUseCase {

    private int status;
    private String body;

    public FailsForPromotion(ErpStubDriver driver) {
        super(driver);
    }

    public FailsForPromotion status(int status) {
        this.status = status;
        return this;
    }

    public FailsForPromotion body(String body) {
        this.body = body;
        return this;
    }

    @Override
    public void execute() {
        driver.stubPromotionError(status, body);
    }
}
