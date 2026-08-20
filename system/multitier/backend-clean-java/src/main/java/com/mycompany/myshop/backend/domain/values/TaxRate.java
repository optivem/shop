package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

// The tax service's answer for one country. countryName is deliberately not guarded: the gateway
// echoes back whatever the service named, and the parity tests assert on that echo.
public record TaxRate(Country countryName, Rate rate) {

    public TaxRate {
        Guard.notNull(rate, "rate");
    }
}
