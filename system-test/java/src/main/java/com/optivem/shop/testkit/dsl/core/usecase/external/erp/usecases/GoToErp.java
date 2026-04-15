package com.optivem.shop.testkit.dsl.core.usecase.external.erp.usecases;

import com.optivem.shop.testkit.driver.port.external.erp.ErpDriver;
import com.optivem.shop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;

public class GoToErp extends BaseErpUseCase<Void, VoidVerification> {
    public GoToErp(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.goToErp();
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
