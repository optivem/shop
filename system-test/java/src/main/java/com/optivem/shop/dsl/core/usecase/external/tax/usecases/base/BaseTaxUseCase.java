package com.optivem.shop.dsl.core.usecase.external.tax.usecases.base;

import com.optivem.shop.dsl.core.shared.BaseUseCase;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;

public abstract class BaseTaxUseCase<TResponse, TVerification> extends BaseUseCase<TaxDriver, TResponse, TVerification> {
    protected BaseTaxUseCase(TaxDriver driver, UseCaseContext context) {
        super(driver, context);
    }
}
