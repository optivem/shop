package com.mycompany.myshop.backend.usecases.queries.order;

import java.math.BigDecimal;
import java.time.Instant;

// One row of the order list, exactly as the orders table holds it.
//
// No value objects and no Guard: running rows through
// Money, Country and CouponCode only to call .amount() and
// .value() on the way out is the cost this side exists to avoid — and a read path must not be
// able to fail on a write-side invariant. status is the stored string rather than the domain
// enum for the same reason.
public record BrowseOrderHistoryItemResponse(
        String orderNumber,
        Instant orderTimestamp,
        String sku,
        String country,
        int quantity,
        BigDecimal totalPrice,
        String status,
        String appliedCouponCode) { }
