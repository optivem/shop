package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {

    public static final int SCALE = 2;

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    // Every factory and every operation routes through here, so no Money can hold a negative
    // amount -- including a result of minus(), which is therefore a partial operation.
    private Money(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        Guard.notNull(amount, "amount");
        return new Money(amount);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public Money applyRate(Rate rate) {
        return new Money(amount.multiply(rate.value()));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money rounded() {
        return new Money(amount.setScale(SCALE, ROUNDING));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public BigDecimal amount() {
        return amount;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Money money && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
