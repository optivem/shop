package com.optivem.starter.backend.core.entities;

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

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    public Order(String orderNumber, Instant orderTimestamp,
                 String sku, int quantity, BigDecimal unitPrice,
                 BigDecimal totalPrice, OrderStatus status) {
        if (orderTimestamp == null) {
            throw new IllegalArgumentException("orderTimestamp cannot be null");
        }
        if (sku == null) {
            throw new IllegalArgumentException("sku cannot be null");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice cannot be null");
        }
        if (totalPrice == null) {
            throw new IllegalArgumentException("totalPrice cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

        this.orderNumber = orderNumber;
        this.orderTimestamp = orderTimestamp;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.status = status;
    }
}
