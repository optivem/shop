package com.mycompany.myshop.backend.usecases.queries.report;

import java.math.BigDecimal;

// What one coupon actually did: how it was published (usageLimit), what the redemption
// counter says (usedCount), and what the orders that carry it add up to.
//
// The strongest of the three cases, because in memory it is two findAlls and a
// hand-rolled join -- every coupon and every order pulled into the application layer so a
// Map could match appliedCouponCode to code. The database does that join
// with an index.
//
// Same rule as ViewSalesReportRevenueByCountryMonthResponse: no value objects, no Guard. Here the second
// argument bites hardest -- Coupon's constructor rejects a discount rate of zero, so on the
// domain path a single unusable coupon takes the whole report down with it.
//
// orderCount and discountAmount count cancelled orders out, so they can disagree
// with usedCount: a redemption is spent when the order is placed and is not given back when
// it is cancelled. That gap is the report's most interesting column, not a bug.
public record ViewSalesReportCouponEffectivenessResponse(
        String code,
        Integer usageLimit,
        Integer usedCount,
        long orderCount,
        BigDecimal discountAmount) { }
