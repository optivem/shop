package com.optivem.shop.dsl.port.given.steps;

import com.optivem.shop.dsl.port.given.steps.base.GivenStep;

public interface GivenCountry extends GivenStep {
    GivenCountry withCountry(String country);
    GivenCountry withTaxRate(double taxRate);
    GivenCountry withTaxRate(String taxRate);
}
