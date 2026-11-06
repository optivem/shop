package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductsResponse;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

public class GetProducts extends BaseErpUseCase<GetProductsResponse, GetProductsVerification> {
    public GetProducts(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<GetProductsResponse, GetProductsVerification> execute() {
        var result = driver.getProducts();
        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, GetProductsVerification::new);
    }
}
