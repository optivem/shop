package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.base;

import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.BaseStubUseCase;

public abstract class BaseTaxUseCase extends BaseStubUseCase<TaxDriver> {

    protected BaseTaxUseCase(TaxDriver driver) {
        super(driver);
    }
}
