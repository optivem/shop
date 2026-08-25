package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.Guard;
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

    // One decision, so the answer is the return type: being asked to deliver an order that is not
    // PLACED is an ordinary outcome of a duplicated request, not an exceptional one. Returning it
    // means no caller can forget to handle it and no use case has to catch anything.
    public Result<Void, RuleViolation> deliver() {
        if (status != OrderStatus.PLACED) {
            return Result.err(new RuleViolation.NotInStatus(null,
                    "Order cannot be delivered in its current status"));
        }
        status = OrderStatus.DELIVERED;
        return Result.ok(null);
    }

    // Three branches the caller treats differently, so a sealed outcome rather than a Result: an
    // already-cancelled order is a retry and the use case answers it with success. Already-cancelled
    // keeps its own message: it is the one negative case the acceptance suite pins by wording.
    public CancelOutcome cancel() {
        if (status == OrderStatus.CANCELLED) {
            return new CancelOutcome.AlreadyCancelled(
                    new RuleViolation.NotInStatus(null, "Order has already been cancelled"));
        }
        if (status != OrderStatus.PLACED) {
            return new CancelOutcome.NotCancellable(
                    new RuleViolation.NotInStatus(null, "Order cannot be cancelled in its current status"));
        }
        status = OrderStatus.CANCELLED;
        return new CancelOutcome.Cancelled();
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
