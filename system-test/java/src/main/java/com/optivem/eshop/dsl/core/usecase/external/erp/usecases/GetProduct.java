package com.optivem.eshop.dsl.core.usecase.external.erp.usecases;

import com.optivem.eshop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductRequest;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductResponse;
import com.optivem.eshop.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;

public class GetProduct extends BaseErpUseCase<GetProductResponse, GetProductVerification> {
    private String skuParamAlias;

    public GetProduct(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public GetProduct sku(String skuParamAlias) {
        this.skuParamAlias = skuParamAlias;
        return this;
    }

    @Override
    public UseCaseResult<GetProductResponse, GetProductVerification> execute() {
        var sku = context.getParamValue(skuParamAlias);

        var request = GetProductRequest.builder()
                .sku(sku)
                .build();

        var result = driver.getProduct(request);

        return new UseCaseResult<>(result, context, GetProductVerification::new);
    }
}
