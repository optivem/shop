package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

public class ReturnsProduct extends BaseErpUseCase<Void, VoidVerification> {
    private String skuParamAlias;
    private String unitPrice;
    private String weight;

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

    public ReturnsProduct weight(String weight) {
        this.weight = weight;
        return this;
    }

    public ReturnsProduct weight(double weight) {
        return weight(Converter.fromDouble(weight));
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var sku = context.getParamValue(skuParamAlias);

        var request = ReturnsProductRequest.builder()
                .sku(sku)
                .price(unitPrice)
                .weight(weight)
                .build();

        var result = driver.returnsProduct(request);

        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, VoidVerification::new);
    }
}
