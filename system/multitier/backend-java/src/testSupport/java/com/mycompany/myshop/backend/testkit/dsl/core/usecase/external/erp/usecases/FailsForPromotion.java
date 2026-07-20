package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;

/** The ERP is broken for the promotion lookup — a {@code 5xx} rather than an answer. */
public class FailsForPromotion extends BaseErpUseCase {

    private int status;
    private String body;

    public FailsForPromotion(ErpDriver driver) {
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
