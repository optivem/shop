package com.optivem.eshop.dsl.core.usecase.external.erp.usecases;

import com.optivem.eshop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.eshop.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.core.shared.VoidVerification;

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
