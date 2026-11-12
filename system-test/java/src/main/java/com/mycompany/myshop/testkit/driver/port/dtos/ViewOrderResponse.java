package com.mycompany.myshop.testkit.driver.port.dtos;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;

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
    /** Null for an order placed without one (Quick order). */
    private ShippingAddress shippingAddress;
    /** UI only: whether the order details offer Cancel Order; null on the API. */
    private Boolean cancelOffered;
    /** UI only: whether the order details offer Deliver Order; null on the API. */
    private Boolean deliverOffered;
}
