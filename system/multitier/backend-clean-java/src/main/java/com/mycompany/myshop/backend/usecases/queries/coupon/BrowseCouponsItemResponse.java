package com.mycompany.myshop.backend.usecases.queries.coupon;

import java.math.BigDecimal;
import java.time.Instant;

// One row of the coupon list, exactly as the coupons table holds it.
//
// Same rule as BrowseOrderHistoryItemResponse: no value objects, no Guard. Here the second
// argument is the stronger one — Coupon's constructor rejects a discountRate of
// zero, so on the domain path a single bad row makes the whole list endpoint fail. A read model
// reports what is stored; it does not re-litigate how it got there.
public record BrowseCouponsItemResponse(
        String code,
        BigDecimal discountRate,
        Instant validFrom,
        Instant validTo,
        Integer usageLimit,
        Integer usedCount) { }
