package com.optivem.shop.dsl.driver.port.shop.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewOrderResponse {
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
}
