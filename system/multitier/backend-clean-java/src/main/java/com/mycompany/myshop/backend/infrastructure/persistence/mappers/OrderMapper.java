package com.mycompany.myshop.backend.infrastructure.persistence.mappers;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;

/**
 * Maps between the domain {@link Order} and its persisted shape. The surrogate {@code id} travels
 * with the domain object so that a re-save of an order read back from storage updates the existing
 * row rather than inserting a new one.
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderJpaEntity entity) {
        var order = new Order(
                entity.getOrderNumber(),
                entity.getOrderTimestamp(),
                entity.getCountry(),
                entity.getSku(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getBasePrice(),
                entity.getDiscountRate(),
                entity.getDiscountAmount(),
                entity.getSubtotalPrice(),
                entity.getTaxRate(),
                entity.getTaxAmount(),
                entity.getTotalPrice(),
                entity.getStatus(),
                entity.getAppliedCouponCode());
        order.setId(entity.getId());
        return order;
    }

    public static OrderJpaEntity toEntity(Order order) {
        var entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setOrderNumber(order.getOrderNumber());
        entity.setOrderTimestamp(order.getOrderTimestamp());
        entity.setCountry(order.getCountry());
        entity.setSku(order.getSku());
        entity.setQuantity(order.getQuantity());
        entity.setUnitPrice(order.getUnitPrice());
        entity.setBasePrice(order.getBasePrice());
        entity.setDiscountRate(order.getDiscountRate());
        entity.setDiscountAmount(order.getDiscountAmount());
        entity.setSubtotalPrice(order.getSubtotalPrice());
        entity.setTaxRate(order.getTaxRate());
        entity.setTaxAmount(order.getTaxAmount());
        entity.setTotalPrice(order.getTotalPrice());
        entity.setStatus(order.getStatus());
        entity.setAppliedCouponCode(order.getAppliedCouponCode());
        return entity;
    }
}
