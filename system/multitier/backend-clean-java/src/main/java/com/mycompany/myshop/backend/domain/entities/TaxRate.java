package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Rate;

public class TaxRate {

    private final String id;
    private final Country countryName;
    private final Rate rate;

    public TaxRate(String id, Country countryName, Rate rate) {
        Guard.notNull(rate, "rate");
        this.id = id;
        this.countryName = countryName;
        this.rate = rate;
    }

    public String getId() {
        return id;
    }

    public Country getCountryName() {
        return countryName;
    }

    public Rate getRate() {
        return rate;
    }
}
