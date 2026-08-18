package com.mycompany.myshop.backend.usecases.queries;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the coupon list, exactly as the {@code coupons} table holds it.
 *
 * <p>Same rule as {@link OrderListItem}: no value objects, no {@code Guard}. Here the second
 * argument is the stronger one — {@code Coupon}'s constructor rejects a {@code discountRate} of
 * zero, so on the domain path a single bad row makes the whole list endpoint fail. A read model
 * reports what is stored; it does not re-litigate how it got there.
 */
public record CouponListItem(
        String code,
        BigDecimal discountRate,
        Instant validFrom,
        Instant validTo,
        Integer usageLimit,
        Integer usedCount) { }
