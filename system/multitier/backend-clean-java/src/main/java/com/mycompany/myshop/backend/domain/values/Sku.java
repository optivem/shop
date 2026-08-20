package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

public record Sku(String value) {

    public static final String FIELD_NAME = "sku";

    // notNull rather than notNullOrEmpty: the format is the ERP's to define, and emptiness is
    // rejected at the use case boundary where it can be reported as a validation failure.
    public Sku {
        Guard.notNull(value, FIELD_NAME);
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
