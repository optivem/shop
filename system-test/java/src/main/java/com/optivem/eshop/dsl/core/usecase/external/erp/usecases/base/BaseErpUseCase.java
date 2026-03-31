package com.optivem.eshop.dsl.core.usecase.external.erp.usecases.base;

import com.optivem.eshop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.eshop.dsl.core.shared.BaseUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;

public abstract class BaseErpUseCase<TResponse, TVerification> extends BaseUseCase<ErpDriver, TResponse, TVerification> {
    protected BaseErpUseCase(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }
}
