package com.mycompany.myshop.backend.infrastructure.persistence.entities;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
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
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "order_timestamp", nullable = false)
    private Instant orderTimestamp;

    @Column(name = "country", nullable = false)
    @ColumnDefault("'US'")
    private String country;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0")
    private BigDecimal basePrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 4)
    @ColumnDefault("0")
    private BigDecimal discountRate;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0")
    private BigDecimal discountAmount;

    @Column(name = "subtotal_price", nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0")
    private BigDecimal subtotalPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    @ColumnDefault("0")
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0")
    private BigDecimal taxAmount;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "applied_coupon_code", nullable = true)
    private String appliedCouponCode;
}
