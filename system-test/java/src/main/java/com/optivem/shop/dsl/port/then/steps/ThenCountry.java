package com.optivem.shop.dsl.port.then.steps;

import com.optivem.shop.dsl.port.then.steps.base.ThenStep;

public interface ThenCountry extends ThenStep<ThenCountry> {
    ThenCountry hasCountry(String country);

    ThenCountry hasTaxRate(double taxRate);

    ThenCountry hasTaxRateIsPositive();
}
