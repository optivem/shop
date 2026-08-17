package com.mycompany.myshop.backend.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An order. A plain object: no ORM annotations, no Spring, no Lombok — the persisted shape lives in
 * {@code infrastructure.persistence.entities.OrderJpaEntity} and the repository adapter maps across.
 *
 * <p>The status transitions are still written as {@code if} blocks in the use cases; Step 8 of the
 * refactor plan moves them onto {@code cancel()} / {@code deliver()} here.
 */
public class Order {

    private Long id;
    private String orderNumber;
    private Instant orderTimestamp;
    private String country;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal basePrice;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private BigDecimal subtotalPrice;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String appliedCouponCode;

    public Order() {
    }

    // one arg per persisted orders column — wide list is intrinsic to the entity mapping
    @SuppressWarnings("java:S107")
    public Order(String orderNumber, Instant orderTimestamp, String country,
                 String sku, int quantity, BigDecimal unitPrice, BigDecimal basePrice,
                 BigDecimal discountRate, BigDecimal discountAmount, BigDecimal subtotalPrice,
                 BigDecimal taxRate, BigDecimal taxAmount, BigDecimal totalPrice, OrderStatus status,
                 String appliedCouponCode) {
        requireNotNull(orderTimestamp, "orderTimestamp");
        requireNotNull(country, "country");
        requireNotNull(sku, "sku");
        requireNotNull(unitPrice, "unitPrice");
        requireNotNull(basePrice, "basePrice");
        requireNotNull(discountRate, "discountRate");
        requireNotNull(discountAmount, "discountAmount");
        requireNotNull(subtotalPrice, "subtotalPrice");
        requireNotNull(taxRate, "taxRate");
        requireNotNull(taxAmount, "taxAmount");
        requireNotNull(totalPrice, "totalPrice");
        requireNotNull(status, "status");

        this.orderNumber = orderNumber;
        this.orderTimestamp = orderTimestamp;
        this.country = country;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.basePrice = basePrice;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
        this.subtotalPrice = subtotalPrice;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.totalPrice = totalPrice;
        this.status = status;
        this.appliedCouponCode = appliedCouponCode;
    }

    private static void requireNotNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Instant getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(Instant orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getSubtotalPrice() {
        return subtotalPrice;
    }

    public void setSubtotalPrice(BigDecimal subtotalPrice) {
        this.subtotalPrice = subtotalPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getAppliedCouponCode() {
        return appliedCouponCode;
    }

    public void setAppliedCouponCode(String appliedCouponCode) {
        this.appliedCouponCode = appliedCouponCode;
    }
}
