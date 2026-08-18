package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;

public class GoToErp extends BaseErpUseCase {

    public GoToErp(ErpDriver driver) {
        super(driver);
    }

    @Override
    public void execute() {
        driver.goToErp();
    }
}
