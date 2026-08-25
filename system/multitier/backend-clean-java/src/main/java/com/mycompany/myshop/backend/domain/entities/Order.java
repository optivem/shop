package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.rules.RuleViolation;
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

    // An Order comes into existence in one of two ways, and neither of them is `new`. `place` is the
    // domain's verb for the transition, sitting beside `deliver` and `cancel` instead of being spelled
    // out at the call site: a placed order starts PLACED, and the caller does not get to say otherwise.
    // `restore` is the mapper putting back an order that already exists, in whatever status was stored.
    // Splitting them is what turns the starting status from an argument into a rule.
    public static Order place(OrderNumber orderNumber, Instant orderTimestamp, Country country, Sku sku,
                              OrderPricing pricing, CouponCode appliedCouponCode) {
        return new Order(orderNumber, orderTimestamp, country, sku, pricing, OrderStatus.PLACED,
                appliedCouponCode);
    }

    public static Order restore(OrderNumber orderNumber, Instant orderTimestamp, Country country, Sku sku,
                                OrderPricing pricing, OrderStatus status, CouponCode appliedCouponCode) {
        return new Order(orderNumber, orderTimestamp, country, sku, pricing, status, appliedCouponCode);
    }

    private Order(OrderNumber orderNumber, Instant orderTimestamp, Country country, Sku sku,
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

    // A refusal is thrown, not returned, because no caller does anything with it but stop: every
    // one of them answers a refused transition with the same 422 carrying the same message. A
    // returned Result would make each caller write out that "stop" by hand, at every call site, to
    // arrive where the throw arrives on its own -- see RefusalTranslatingUseCase for where it lands.
    //
    // The wording stays here, in the object that owns the rule. What travels is a RuleViolation, so
    // the single translation at the boundary is a total switch over a sealed type rather than a
    // string being sniffed.
    public void deliver() {
        if (status != OrderStatus.PLACED) {
            throw new ValidationException(
                    new RuleViolation.NotInStatus("Order cannot be delivered in its current status"));
        }
        status = OrderStatus.DELIVERED;
    }

    // Two refusals, and CancelOrder treats them identically -- both become a 422 carrying this
    // wording. That is why they are thrown rather than handed back as a sealed CancelOutcome the
    // caller pattern-matches: a branch nobody takes is a branch that costs a type, a switch and a
    // reader's attention to describe a difference that never shows up in behaviour.
    //
    // Already-cancelled keeps its own message: it is the one negative case the acceptance suite pins
    // by wording. If it ever needs to become an idempotent success, that is the day it earns a
    // distinct type -- and RuleViolation.NotInStatus already carries enough for the caller to tell.
    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new ValidationException(
                    new RuleViolation.NotInStatus("Order has already been cancelled"));
        }
        if (status != OrderStatus.PLACED) {
            throw new ValidationException(
                    new RuleViolation.NotInStatus("Order cannot be cancelled in its current status"));
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
