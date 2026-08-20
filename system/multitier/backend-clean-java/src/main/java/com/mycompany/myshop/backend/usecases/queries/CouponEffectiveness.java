package com.mycompany.myshop.backend.usecases.queries;

import java.math.BigDecimal;

/**
 * What one coupon actually did: how it was published ({@code usageLimit}), what the redemption
 * counter says ({@code usedCount}), and what the orders that carry it add up to.
 *
 * <p>The strongest of the three cases, because in memory it is two {@code findAll}s and a
 * hand-rolled join -- every coupon and every order pulled into the application layer so a
 * {@code Map} could match {@code appliedCouponCode} to {@code code}. The database does that join
 * with an index.
 *
 * <p>Same rule as {@link RevenueByCountryMonth}: no value objects, no {@code Guard}. Here the second
 * argument bites hardest -- {@code Coupon}'s constructor rejects a discount rate of zero, so on the
 * domain path a single unusable coupon takes the whole report down with it.
 *
 * <p>{@code orderCount} and {@code discountAmount} count cancelled orders out, so they can disagree
 * with {@code usedCount}: a redemption is spent when the order is placed and is not given back when
 * it is cancelled. That gap is the report's most interesting column, not a bug.
 */
public record CouponEffectiveness(
        String code,
        Integer usageLimit,
        Integer usedCount,
        long orderCount,
        BigDecimal discountAmount) { }
