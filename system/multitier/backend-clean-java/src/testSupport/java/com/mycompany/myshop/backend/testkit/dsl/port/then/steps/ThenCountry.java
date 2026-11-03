package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCountry extends ThenStep<ThenCountry> {
    ThenCountry hasTaxRate(String expectedTaxRate);
}
