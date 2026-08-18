package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

import java.util.Optional;

public record CouponCode(String value) {

    public static final String FIELD_NAME = "couponCode";

    public CouponCode {
        Guard.notNullOrEmpty(value, "code");
    }

    public static CouponCode of(String value) {
        return new CouponCode(value);
    }

    public static Optional<CouponCode> requested(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CouponCode(value));
    }

    public static String valueOrNull(CouponCode code) {
        return code == null ? null : code.value();
    }

    @Override
    public String toString() {
        return value;
    }
}
