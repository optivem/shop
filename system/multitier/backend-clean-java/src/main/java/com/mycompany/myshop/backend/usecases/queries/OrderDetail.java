package com.mycompany.myshop.backend.usecases.queries;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One order, every field of it a column in {@code orders}.
 *
 * <p>The sharpest of the three: fifteen fields, and the domain path reached them by constructing
 * seven {@code Money}, two {@code Rate}, a {@code Country} and a {@code CouponCode} and then
 * unwrapping every one of them. Nothing built on that path survived to the wire.
 *
 * <p>Same rule as {@link OrderListItem}: no value objects, no {@code Guard}.
 */
public record OrderDetail(
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
