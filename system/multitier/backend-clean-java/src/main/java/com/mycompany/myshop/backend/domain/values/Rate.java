package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Rate {

    public static final int SCALE = 4;

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final Rate ZERO = new Rate(BigDecimal.ZERO);
    public static final Rate ONE = new Rate(BigDecimal.ONE);

    private final BigDecimal value;

    private Rate(BigDecimal value) {
        this.value = value;
    }

    public static Rate of(BigDecimal value) {
        Guard.notNull(value, "rate");
        return new Rate(value);
    }

    public static Rate of(String value) {
        return of(new BigDecimal(value));
    }

    public Rate rounded() {
        return new Rate(value.setScale(SCALE, ROUNDING));
    }

    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreaterThan(Rate other) {
        return value.compareTo(other.value) > 0;
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Rate rate && value.compareTo(rate.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
