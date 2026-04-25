package com.mycompany.myshop.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "order_timestamp", nullable = false)
    private Instant orderTimestamp;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "subtotal_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "applied_coupon_code", nullable = true)
    private String appliedCouponCode;

    public Order(String orderNumber, Instant orderTimestamp, String country,
                 String sku, int quantity, BigDecimal unitPrice, BigDecimal basePrice,
                 BigDecimal discountRate, BigDecimal discountAmount, BigDecimal subtotalPrice,
                 BigDecimal taxRate, BigDecimal taxAmount, BigDecimal totalPrice, OrderStatus status,
                 String appliedCouponCode) {
        if (orderTimestamp == null) {
            throw new IllegalArgumentException("orderTimestamp cannot be null");
        }
        if (country == null) {
            throw new IllegalArgumentException("country cannot be null");
        }
        if (sku == null) {
            throw new IllegalArgumentException("sku cannot be null");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice cannot be null");
        }
        if (basePrice == null) {
            throw new IllegalArgumentException("basePrice cannot be null");
        }
        if (discountRate == null) {
            throw new IllegalArgumentException("discountRate cannot be null");
        }
        if (discountAmount == null) {
            throw new IllegalArgumentException("discountAmount cannot be null");
        }
        if (subtotalPrice == null) {
            throw new IllegalArgumentException("subtotalPrice cannot be null");
        }
        if (taxRate == null) {
            throw new IllegalArgumentException("taxRate cannot be null");
        }
        if (taxAmount == null) {
            throw new IllegalArgumentException("taxAmount cannot be null");
        }
        if (totalPrice == null) {
            throw new IllegalArgumentException("totalPrice cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

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
}
