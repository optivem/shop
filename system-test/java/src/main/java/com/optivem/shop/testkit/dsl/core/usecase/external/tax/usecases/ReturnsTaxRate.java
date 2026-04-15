package com.optivem.shop.testkit.dsl.core.usecase.external.tax.usecases;

import com.optivem.shop.testkit.common.Converter;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;
import com.optivem.shop.testkit.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;
import com.optivem.shop.testkit.driver.port.external.tax.TaxDriver;
import com.optivem.shop.testkit.driver.port.external.tax.dtos.ReturnsTaxRateRequest;

public class ReturnsTaxRate extends BaseTaxUseCase<Void, VoidVerification> {
    private String countryAlias;
    private String taxRate;

    public ReturnsTaxRate(TaxDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsTaxRate country(String countryAlias) {
        this.countryAlias = countryAlias;
        return this;
    }

    public ReturnsTaxRate taxRate(String taxRate) {
        this.taxRate = taxRate;
        return this;
    }

    public ReturnsTaxRate taxRate(double taxRate) {
        return taxRate(Converter.fromDouble(taxRate));
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var country = context.getParamValueOrLiteral(countryAlias);

        var request = ReturnsTaxRateRequest.builder()
                .country(country)
                .taxRate(taxRate)
                .build();

        var result = driver.returnsTaxRate(request);

        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
