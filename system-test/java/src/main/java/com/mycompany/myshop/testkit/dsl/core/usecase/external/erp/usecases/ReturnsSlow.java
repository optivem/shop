package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

/** Makes the ERP slow: every ERP API call answers only after the system's outbound timeout has passed. */
public class ReturnsSlow extends BaseErpUseCase<Void, VoidVerification> {
    public ReturnsSlow(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.returnsSlow();

        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, VoidVerification::new);
    }
}
