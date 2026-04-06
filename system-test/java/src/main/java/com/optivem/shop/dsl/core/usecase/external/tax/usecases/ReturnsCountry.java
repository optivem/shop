package com.optivem.shop.dsl.core.usecase.external.tax.usecases;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.core.shared.UseCaseResult;
import com.optivem.shop.dsl.core.shared.VoidVerification;
import com.optivem.shop.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.ReturnsCountryRequest;

public class ReturnsCountry extends BaseTaxUseCase<Void, VoidVerification> {
    private String country;
    private String taxRate;

    public ReturnsCountry(TaxDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsCountry withCountry(String country) {
        this.country = country;
        return this;
    }

    public ReturnsCountry withTaxRate(String taxRate) {
        this.taxRate = taxRate;
        return this;
    }

    public ReturnsCountry withTaxRate(double taxRate) {
        return withTaxRate(Converter.fromDouble(taxRate));
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = ReturnsCountryRequest.builder()
                .country(country)
                .taxRate(taxRate)
                .build();

        var result = driver.returnsCountry(request);
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
