package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Rate;

public class TaxRate {

    private final Country countryName;
    private final Rate rate;

    public TaxRate(Country countryName, Rate rate) {
        Guard.notNull(rate, "rate");
        this.countryName = countryName;
        this.rate = rate;
    }

    public Country getCountryName() {
        return countryName;
    }

    public Rate getRate() {
        return rate;
    }
}
