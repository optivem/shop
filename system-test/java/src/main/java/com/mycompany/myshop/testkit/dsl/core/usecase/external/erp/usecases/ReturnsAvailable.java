package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

/** Brings the ERP back after an outage: its calls answer with what is staged again. */
public class ReturnsAvailable extends BaseErpUseCase<Void, VoidVerification> {
    public ReturnsAvailable(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.returnsAvailable();

        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, VoidVerification::new);
    }
}
