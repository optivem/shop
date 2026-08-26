package com.mycompany.myshop.backend.usecases.queries.order;

import java.math.BigDecimal;
import java.time.Instant;

// One order, every field of it a column in orders.
//
// The sharpest of the three: fifteen fields, and the domain path reached them by constructing
// seven Money, two Rate, a Country and a CouponCode and then
// unwrapping every one of them. Nothing built on that path survived to the wire.
//
// Same rule as BrowseOrderHistoryItemResponse: no value objects, no Guard.
public record ViewOrderDetailsResponse(
        String orderNumber,
        Instant orderTimestamp,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal basePrice,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        BigDecimal subtotalPrice,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalPrice,
        String status,
        String country,
        String appliedCouponCode) { }
