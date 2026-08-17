package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

/**
 * The country an order is placed from, and the country a tax rate belongs to. One concept that had
 * been spelled two ways: {@code country} on
 * {@link com.mycompany.myshop.backend.domain.entities.Order} and {@code countryName} on
 * {@link com.mycompany.myshop.backend.domain.entities.TaxRate}, both bare strings, with nothing
 * saying they were the same thing or could be compared.
 *
 * <p>It is also the key the tax system is queried by, which is what makes the type worth having: the
 * value reaches an outbound URL in {@code infrastructure.external.tax.HttpTaxGateway}, so there is
 * exactly one place a rule about what a country may look like would have to go.
 *
 * <p>Deliberately does <em>not</em> normalise case or trim. Whether {@code "us"} and {@code "US"} name
 * the same country is currently the tax system's answer to give, not this type's, and quietly
 * changing that would change which orders can be placed.
 */
public record Country(String value) {

    public Country {
        Guard.notNullOrEmpty(value, "country");
    }

    public static Country of(String value) {
        return new Country(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
