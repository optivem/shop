package com.mycompany.myshop.backend.infrastructure.persistence.mappers;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderJpaEntity entity) {
        var pricing = new OrderPricing(
                Money.of(entity.getUnitPrice()),
                entity.getQuantity(),
                Money.of(entity.getBasePrice()),
                Rate.of(entity.getDiscountRate()),
                Money.of(entity.getDiscountAmount()),
                Money.of(entity.getSubtotalPrice()),
                Rate.of(entity.getTaxRate()),
                Money.of(entity.getTaxAmount()),
                Money.of(entity.getTotalPrice()));

        return new Order(
                entity.getOrderNumber(),
                entity.getOrderTimestamp(),
                Country.of(entity.getCountry()),
                entity.getSku(),
                pricing,
                entity.getStatus(),
                // Nullable column: an order placed without a coupon has none.
                CouponCode.requested(entity.getAppliedCouponCode()).orElse(null));
    }

    public static OrderJpaEntity toEntity(Order order) {
        var entity = new OrderJpaEntity();
        entity.setOrderNumber(order.getOrderNumber());
        entity.setOrderTimestamp(order.getOrderTimestamp());
        entity.setCountry(order.getCountry().value());
        entity.setSku(order.getSku());
        entity.setQuantity(order.getQuantity());
        entity.setUnitPrice(order.getUnitPrice().amount());
        entity.setBasePrice(order.getBasePrice().amount());
        entity.setDiscountRate(order.getDiscountRate().value());
        entity.setDiscountAmount(order.getDiscountAmount().amount());
        entity.setSubtotalPrice(order.getSubtotalPrice().amount());
        entity.setTaxRate(order.getTaxRate().value());
        entity.setTaxAmount(order.getTaxAmount().amount());
        entity.setTotalPrice(order.getTotalPrice().amount());
        entity.setStatus(order.getStatus());
        var appliedCouponCode = order.getAppliedCouponCode();
        entity.setAppliedCouponCode(appliedCouponCode == null ? null : appliedCouponCode.value());
        return entity;
    }
}
