package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

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
