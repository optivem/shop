package com.mycompany.myshop.backend.domain.values;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A monetary amount. Owns the arithmetic that used to sit as loose {@code BigDecimal} locals in
 * {@code placeOrder}, and owns the rounding policy: amounts are carried exactly through a
 * calculation and rounded to {@link #SCALE} decimal places once, at {@link #rounded()}, when the
 * result is recorded on an order.
 *
 * <p>Rounding once at the end rather than after every step is deliberate, and is what makes this
 * refactor value-preserving: the CRUD variant let {@code DECIMAL(10,2)} round each column
 * independently on write, so rounding an exact intermediate chain reproduces its numbers exactly,
 * whereas feeding each step a pre-rounded input would drift by a cent.
 */
public final class Money {

    /** Decimal places a recorded amount is carried at — the same as the persisted {@code DECIMAL(10,2)}. */
    public static final int SCALE = 2;

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        return new Money(amount);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    /** This amount repeated {@code quantity} times. */
    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    /** The portion of this amount that {@code rate} describes — a discount, a tax, a promotion factor. */
    public Money applyRate(Rate rate) {
        return new Money(amount.multiply(rate.value()));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** This amount at the scale money is recorded at. */
    public Money rounded() {
        return new Money(amount.setScale(SCALE, ROUNDING));
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
