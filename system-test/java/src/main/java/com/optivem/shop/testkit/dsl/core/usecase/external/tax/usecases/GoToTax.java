package com.optivem.shop.testkit.dsl.core.usecase.external.tax.usecases;

import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;
import com.optivem.shop.testkit.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;
import com.optivem.shop.testkit.driver.port.external.tax.TaxDriver;

public class GoToTax extends BaseTaxUseCase<Void, VoidVerification> {
    public GoToTax(TaxDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.goToTax();
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
