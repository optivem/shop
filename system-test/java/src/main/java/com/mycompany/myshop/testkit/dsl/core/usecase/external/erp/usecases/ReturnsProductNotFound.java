package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductRequest;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;

/** The ERP no longer offers a product: asking for it by SKU (an alias) answers "not found". */
public class ReturnsProductNotFound extends BaseErpUseCase<Void, VoidVerification> {
    private String skuAlias;

    public ReturnsProductNotFound(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsProductNotFound sku(String skuAlias) {
        this.skuAlias = skuAlias;
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = GetProductRequest.builder().sku(context.getParamValue(skuAlias)).build();
        var result = driver.returnsProductNotFound(request);

        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, VoidVerification::new);
    }
}
