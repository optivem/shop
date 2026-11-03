package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCountry extends ThenStep<ThenCountry> {
    ThenCountry hasCountry(String country);

    ThenCountry hasTaxRate(String taxRate);

    ThenCountry hasTaxRateIsPositive();
}
