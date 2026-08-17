package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

import java.util.Optional;

/**
 * A coupon's business identifier — the thing a shopper types, the key coupons are looked up by, and
 * the field an invalid one is reported against.
 *
 * <p>The surrogate {@code id} on {@link com.mycompany.myshop.backend.domain.entities.Coupon} is
 * storage's; this is the domain's. Making it a type is what lets the two readings of a blank code sit
 * side by side without either being mistaken for the other: {@link #of(String)} rejects one, because
 * a coupon cannot be published without a code, while {@link #requested(String)} treats it as "no
 * coupon offered", because an order placed without one is ordinary. Both readings were previously
 * spelled as the same {@code null || trim().isEmpty()} check in different classes, meaning opposite
 * things.
 */
public record CouponCode(String value) {

    /**
     * The name this code is reported under when it fails validation. Held here because the field name
     * and the value are the same concept; it had been a private constant in three separate classes.
     */
    public static final String FIELD_NAME = "couponCode";

    public CouponCode {
        Guard.notNullOrEmpty(value, "code");
    }

    public static CouponCode of(String value) {
        return new CouponCode(value);
    }

    /**
     * The code a request asked for, if it asked for one: empty when absent or blank. Callers that
     * treat "no coupon" as ordinary use this; callers that require a code use {@link #of(String)}.
     */
    public static Optional<CouponCode> requested(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CouponCode(value));
    }

    /**
     * The value of a code that may be absent. The response DTOs carry plain strings and an order
     * placed without a coupon has no code, so the unwrap is null-safe — stated once here rather than
     * as the same private helper in every use case that echoes a coupon back.
     */
    public static String valueOrNull(CouponCode code) {
        return code == null ? null : code.value();
    }

    @Override
    public String toString() {
        return value;
    }
}
