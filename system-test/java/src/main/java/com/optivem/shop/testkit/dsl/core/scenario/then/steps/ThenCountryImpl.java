package com.optivem.shop.testkit.dsl.core.scenario.then.steps;

import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.usecase.external.tax.usecases.GetTaxVerification;
import com.optivem.shop.testkit.dsl.port.then.steps.ThenCountry;

public class ThenCountryImpl extends BaseThenStep<Void, VoidVerification> implements ThenCountry {
    private final GetTaxVerification verification;

    public ThenCountryImpl(UseCaseDsl app, ExecutionResultContext executionResult, GetTaxVerification verification) {
        super(app, executionResult, null);
        this.verification = verification;
    }

    @Override
    public ThenCountryImpl hasCountry(String country) {
        verification.country(country);
        return this;
    }

    @Override
    public ThenCountryImpl hasTaxRate(double taxRate) {
        verification.taxRate(taxRate);
        return this;
    }

    @Override
    public ThenCountryImpl hasTaxRateIsPositive() {
        verification.taxRateIsPositive();
        return this;
    }

    @Override
    public ThenCountryImpl and() {
        return this;
    }
}
