package com.mycompany.myshop.backend.usecases.queries;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the order list, exactly as the {@code orders} table holds it.
 *
 * <p>No value objects and no {@code Guard}: running rows through
 * {@code Money}, {@code Country} and {@code CouponCode} only to call {@code .amount()} and
 * {@code .value()} on the way out is the cost this side exists to avoid — and a read path must not be
 * able to fail on a write-side invariant. {@code status} is the stored string rather than the domain
 * enum for the same reason.
 */
public record OrderListItem(
        String orderNumber,
        Instant orderTimestamp,
        String sku,
        String country,
        int quantity,
        BigDecimal totalPrice,
        String status,
        String appliedCouponCode) { }
