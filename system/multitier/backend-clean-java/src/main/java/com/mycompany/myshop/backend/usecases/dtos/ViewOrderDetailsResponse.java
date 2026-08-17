package com.mycompany.myshop.backend.usecases.dtos;

import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ViewOrderDetailsResponse {
    private String orderNumber;
    private Instant orderTimestamp;
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
    private String country;
    private String appliedCouponCode;
}
