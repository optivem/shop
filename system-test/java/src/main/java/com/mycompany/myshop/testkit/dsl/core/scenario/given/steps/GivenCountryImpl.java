package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenCountry;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class GivenCountryImpl extends BaseGivenStep implements GivenCountry {
    private String country;
    private String taxRate;

    public GivenCountryImpl(GivenImpl given) {
        super(given);
        withCode(DEFAULT_COUNTRY);
        withTaxRate(DEFAULT_TAX_RATE);
    }

    @Override
    public GivenCountryImpl withCode(String country) {
        this.country = country;
        return this;
    }

    @Override
    public GivenCountryImpl withTaxRate(String taxRate) {
        this.taxRate = taxRate;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.tax().returnsTaxRate()
                .country(country)
                .taxRate(taxRate)
                .execute()
                .shouldSucceed();
    }
}
