package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Rate;

/**
 * A country's tax rate as the domain understands it. The tax system's wire representation lives in
 * {@code infrastructure.external.tax} and is mapped to this by the gateway adapter.
 */
public class TaxRate {

    private final String id;
    private final String countryName;
    private final Rate rate;

    public TaxRate(String id, String countryName, Rate rate) {
        Guard.notNull(rate, "rate");
        this.id = id;
        this.countryName = countryName;
        this.rate = rate;
    }

    public String getId() {
        return id;
    }

    public String getCountryName() {
        return countryName;
    }

    public Rate getRate() {
        return rate;
    }
}
