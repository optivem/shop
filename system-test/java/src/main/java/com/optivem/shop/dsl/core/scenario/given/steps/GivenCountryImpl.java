package com.optivem.shop.dsl.core.scenario.given.steps;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.core.scenario.given.GivenImpl;
import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.port.given.steps.GivenCountry;

public class GivenCountryImpl extends BaseGivenStep implements GivenCountry {
    private String country;
    private String taxRate;

    public GivenCountryImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenCountryImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    @Override
    public GivenCountryImpl withTaxRate(double taxRate) {
        return withTaxRate(Converter.fromDouble(taxRate));
    }

    @Override
    public GivenCountryImpl withTaxRate(String taxRate) {
        this.taxRate = taxRate;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.tax().returnsCountry()
                .withCountry(country)
                .withTaxRate(taxRate)
                .execute()
                .shouldSucceed();
    }
}
