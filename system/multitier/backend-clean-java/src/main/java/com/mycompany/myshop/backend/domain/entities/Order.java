package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Sku;

import java.time.Instant;

public class Order {

    private final OrderNumber orderNumber;
    private final Instant orderTimestamp;
    private final Country country;
    private final Sku sku;
    private final OrderPricing pricing;
    private OrderStatus status;
    private final CouponCode appliedCouponCode;

    public Order(OrderNumber orderNumber, Instant orderTimestamp, Country country, Sku sku,
                 OrderPricing pricing, OrderStatus status, CouponCode appliedCouponCode) {
        Guard.notNull(orderNumber, OrderNumber.FIELD_NAME);
        Guard.notNull(orderTimestamp, "orderTimestamp");
        Guard.notNull(country, "country");
        Guard.notNull(sku, Sku.FIELD_NAME);
        Guard.notNull(pricing, "pricing");
        Guard.notNull(status, "status");

        this.orderNumber = orderNumber;
        this.orderTimestamp = orderTimestamp;
        this.country = country;
        this.sku = sku;
        this.pricing = pricing;
        this.status = status;
        this.appliedCouponCode = appliedCouponCode;
    }

    public void deliver() {
        if (status != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be delivered in its current status");
        }
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        // Already-cancelled keeps its own message: it is the one negative case the acceptance suite
        // pins by wording.
        if (status == OrderStatus.CANCELLED) {
            throw new ValidationException("Order has already been cancelled");
        }
        if (status != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be cancelled in its current status");
        }
        status = OrderStatus.CANCELLED;
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public Instant getOrderTimestamp() {
        return orderTimestamp;
    }

    public Country getCountry() {
        return country;
    }

    public Sku getSku() {
        return sku;
    }

    public OrderPricing getPricing() {
        return pricing;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public CouponCode getAppliedCouponCode() {
        return appliedCouponCode;
    }
}
