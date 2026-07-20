package com.mycompany.myshop.backend.support.core.scenario.given.steps;

import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.given.steps.GivenCountry;
import java.math.BigDecimal;

public class GivenCountryImpl extends BaseGivenStep implements GivenCountry {

    private String country;
    private String taxRate;
    private boolean exists = true;

    public GivenCountryImpl(GivenImpl given) {
        super(given);
        withCode(ScenarioDefaults.DEFAULT_COUNTRY);
        withTaxRate(ScenarioDefaults.DEFAULT_TAX_RATE);
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
    public GivenCountryImpl withTaxRate(double taxRate) {
        return withTaxRate(BigDecimal.valueOf(taxRate).toPlainString());
    }

    @Override
    public GivenCountryImpl doesNotExist() {
        this.exists = false;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        if (exists) {
            app.tax().returnsTaxRate().country(country).taxRate(taxRate).execute();
        } else {
            app.tax().returnsNoTaxRate().country(country).execute();
        }
    }
}
