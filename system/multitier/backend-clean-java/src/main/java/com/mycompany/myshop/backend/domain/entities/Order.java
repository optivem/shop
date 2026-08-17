package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;

import java.time.Instant;

/**
 * An order. A plain object: no ORM annotations, no Spring, no Lombok — the persisted shape lives in
 * {@code infrastructure.persistence.entities.OrderJpaEntity} and the repository adapter maps across.
 *
 * <p>It owns its own status transitions. {@link #cancel()} and {@link #deliver()} are the only ways
 * the status moves, so the rule about what may follow what is stated once here rather than as an
 * {@code if} block in each use case that happens to need it.
 */
public class Order {

    private Long id;
    private final String orderNumber;
    private final Instant orderTimestamp;
    private final String country;
    private final String sku;
    private final OrderPricing pricing;
    private OrderStatus status;
    private final String appliedCouponCode;

    public Order(String orderNumber, Instant orderTimestamp, String country, String sku,
                 OrderPricing pricing, OrderStatus status, String appliedCouponCode) {
        Guard.notNull(orderTimestamp, "orderTimestamp");
        Guard.notNull(country, "country");
        Guard.notNull(sku, "sku");
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

    /** Only a placed order can be delivered. */
    public void deliver() {
        if (status != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be delivered in its current status");
        }
        status = OrderStatus.DELIVERED;
    }

    /** An order can be cancelled from any status but cancelled. */
    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new ValidationException("Order has already been cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    /** Set by the repository adapter once storage has assigned the row its identity. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Instant getOrderTimestamp() {
        return orderTimestamp;
    }

    public String getCountry() {
        return country;
    }

    public String getSku() {
        return sku;
    }

    public OrderPricing getPricing() {
        return pricing;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getAppliedCouponCode() {
        return appliedCouponCode;
    }

    public int getQuantity() {
        return pricing.quantity();
    }

    public Money getUnitPrice() {
        return pricing.unitPrice();
    }

    public Money getBasePrice() {
        return pricing.basePrice();
    }

    public Rate getDiscountRate() {
        return pricing.discountRate();
    }

    public Money getDiscountAmount() {
        return pricing.discountAmount();
    }

    public Money getSubtotalPrice() {
        return pricing.subtotalPrice();
    }

    public Rate getTaxRate() {
        return pricing.taxRate();
    }

    public Money getTaxAmount() {
        return pricing.taxAmount();
    }

    public Money getTotalPrice() {
        return pricing.totalPrice();
    }
}
