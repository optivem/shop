package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;

import java.time.Instant;

public class Order {

    private final String orderNumber;
    private final Instant orderTimestamp;
    private final Country country;
    private final String sku;
    private final OrderPricing pricing;
    private OrderStatus status;
    private final CouponCode appliedCouponCode;

    public Order(String orderNumber, Instant orderTimestamp, Country country, String sku,
                 OrderPricing pricing, OrderStatus status, CouponCode appliedCouponCode) {
        Guard.notNull(orderNumber, "orderNumber");
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

    public void deliver() {
        if (status != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be delivered in its current status");
        }
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new ValidationException("Order has already been cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Instant getOrderTimestamp() {
        return orderTimestamp;
    }

    public Country getCountry() {
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

    public CouponCode getAppliedCouponCode() {
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
