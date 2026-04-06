package com.optivem.shop.dsl.core.usecase.external.tax.usecases;

import com.optivem.shop.dsl.core.shared.ResponseVerification;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.driver.port.external.tax.dtos.GetCountryResponse;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class GetCountryVerification extends ResponseVerification<GetCountryResponse> {
    public GetCountryVerification(GetCountryResponse response, UseCaseContext context) {
        super(response, context);
    }

    public GetCountryVerification taxRate(BigDecimal expectedTaxRate) {
        var actualTaxRate = getResponse().getTaxRate();
        assertThat(actualTaxRate)
                .withFailMessage("Expected tax rate to be %s, but was %s", expectedTaxRate, actualTaxRate)
                .isEqualByComparingTo(expectedTaxRate);
        return this;
    }

    public GetCountryVerification taxRate(double expectedTaxRate) {
        return taxRate(BigDecimal.valueOf(expectedTaxRate));
    }
}
