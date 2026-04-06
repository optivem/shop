package com.optivem.shop.dsl.core.usecase.external.tax.usecases;

import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.core.shared.UseCaseResult;
import com.optivem.shop.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryRequest;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryResponse;

public class GetCountry extends BaseTaxUseCase<GetCountryResponse, GetCountryVerification> {
    private String country;

    public GetCountry(TaxDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public GetCountry country(String country) {
        this.country = country;
        return this;
    }

    @Override
    public UseCaseResult<GetCountryResponse, GetCountryVerification> execute() {
        var request = GetCountryRequest.builder()
                .country(country)
                .build();

        var result = driver.getCountry(request);
        return new UseCaseResult<>(result, context, GetCountryVerification::new);
    }
}
