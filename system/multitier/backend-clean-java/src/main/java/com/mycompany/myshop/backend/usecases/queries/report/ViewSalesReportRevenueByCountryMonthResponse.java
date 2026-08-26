package com.mycompany.myshop.backend.usecases.queries.report;

import java.math.BigDecimal;
import java.time.Instant;

// Revenue for one country in one month, already summed by the database.
//
// No value objects and no Guard, and the absence is deliberate: these numbers were
// produced by SUM over rows that no longer exist as objects, so there is nothing for a
// Money to wrap that the database did not already compute. Running 100k rows through
// Money, Country and Guard to arrive at figures that go straight to JSON is
// exactly the cost this side exists to avoid -- and a report must not be able to fail on a
// write-side invariant that some historical row no longer satisfies.
//
// month is the first instant of the month, as date_trunc returns it.
public record ViewSalesReportRevenueByCountryMonthResponse(
        String country,
        Instant month,
        long orderCount,
        long quantity,
        BigDecimal subtotalPrice,
        BigDecimal taxAmount,
        BigDecimal totalPrice) { }
