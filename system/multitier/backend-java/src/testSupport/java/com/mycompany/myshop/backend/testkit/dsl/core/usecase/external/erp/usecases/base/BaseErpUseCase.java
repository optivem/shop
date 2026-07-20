package com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.base;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.shared.BaseStubUseCase;

public abstract class BaseErpUseCase extends BaseStubUseCase<ErpStubDriver> {

    protected BaseErpUseCase(ErpStubDriver driver) {
        super(driver);
    }
}
