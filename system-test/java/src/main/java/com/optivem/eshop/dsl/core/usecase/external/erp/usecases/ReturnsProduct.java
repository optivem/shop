package com.optivem.eshop.dsl.core.usecase.external.erp.usecases;

import com.optivem.eshop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.optivem.eshop.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.optivem.eshop.dsl.common.Converter;
import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.core.shared.VoidVerification;

public class ReturnsProduct extends BaseErpUseCase<Void, VoidVerification> {
    private String skuParamAlias;
    private String unitPrice;

    public ReturnsProduct(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsProduct sku(String skuParamAlias) {
        this.skuParamAlias = skuParamAlias;
        return this;
    }

    public ReturnsProduct unitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public ReturnsProduct unitPrice(double unitPrice) {
        return unitPrice(Converter.fromDouble(unitPrice));
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var sku = context.getParamValue(skuParamAlias);

        var request = ReturnsProductRequest.builder()
                .sku(sku)
                .price(unitPrice)
                .build();

        var result = driver.returnsProduct(request);

        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
