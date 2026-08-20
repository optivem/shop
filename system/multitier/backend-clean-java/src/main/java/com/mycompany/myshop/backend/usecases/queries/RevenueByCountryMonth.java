package com.mycompany.myshop.backend.usecases.queries;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Revenue for one country in one month, already summed by the database.
 *
 * <p>No value objects and no {@code Guard}, and the absence is deliberate: these numbers were
 * produced by {@code SUM} over rows that no longer exist as objects, so there is nothing for a
 * {@code Money} to wrap that the database did not already compute. Running 100k rows through
 * {@code Money}, {@code Country} and {@code Guard} to arrive at figures that go straight to JSON is
 * exactly the cost this side exists to avoid -- and a report must not be able to fail on a
 * write-side invariant that some historical row no longer satisfies.
 *
 * <p>{@code month} is the first instant of the month, as {@code date_trunc} returns it.
 */
public record RevenueByCountryMonth(
        String country,
        Instant month,
        long orderCount,
        long quantity,
        BigDecimal subtotalPrice,
        BigDecimal taxAmount,
        BigDecimal totalPrice) { }
