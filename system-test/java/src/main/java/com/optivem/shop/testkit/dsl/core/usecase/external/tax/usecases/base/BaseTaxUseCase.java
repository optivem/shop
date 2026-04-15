package com.optivem.shop.testkit.dsl.core.usecase.external.tax.usecases.base;

import com.optivem.shop.testkit.dsl.core.shared.BaseUseCase;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.driver.port.external.tax.TaxDriver;

public abstract class BaseTaxUseCase<TResponse, TVerification> extends BaseUseCase<TaxDriver, TResponse, TVerification> {
    protected BaseTaxUseCase(TaxDriver driver, UseCaseContext context) {
        super(driver, context);
    }
}
