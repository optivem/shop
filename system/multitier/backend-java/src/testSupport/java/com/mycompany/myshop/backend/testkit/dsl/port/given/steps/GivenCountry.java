package com.mycompany.myshop.backend.support.port.given.steps;

import com.mycompany.myshop.backend.support.port.given.steps.base.GivenStep;

public interface GivenCountry extends GivenStep {
    GivenCountry withCode(String country);

    GivenCountry withTaxRate(String taxRate);

    GivenCountry withTaxRate(double taxRate);

    GivenCountry doesNotExist();
}
