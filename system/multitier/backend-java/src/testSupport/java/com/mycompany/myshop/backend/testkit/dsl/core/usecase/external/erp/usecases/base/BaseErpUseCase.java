package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.base;

import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.BaseStubUseCase;

public abstract class BaseErpUseCase extends BaseStubUseCase<ErpDriver> {

    protected BaseErpUseCase(ErpDriver driver) {
        super(driver);
    }
}
