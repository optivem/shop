package com.mycompany.myshop.backend.infrastructure.persistence.mappers;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;

/**
 * Maps between the domain {@link Order} and its persisted shape. The surrogate {@code id} travels
 * with the domain object so that a re-save of an order read back from storage updates the existing
 * row rather than inserting a new one.
 *
 * <p>This is also where the domain's {@code Money}/{@code Rate} meet the columns' plain
 * {@code BigDecimal}: the typed values stop at the edge of the ORM, which knows nothing about them.
 */
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

        var order = new Order(
                entity.getOrderNumber(),
                entity.getOrderTimestamp(),
                entity.getCountry(),
                entity.getSku(),
                pricing,
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
        entity.setUnitPrice(order.getUnitPrice().amount());
        entity.setBasePrice(order.getBasePrice().amount());
        entity.setDiscountRate(order.getDiscountRate().value());
        entity.setDiscountAmount(order.getDiscountAmount().amount());
        entity.setSubtotalPrice(order.getSubtotalPrice().amount());
        entity.setTaxRate(order.getTaxRate().value());
        entity.setTaxAmount(order.getTaxAmount().amount());
        entity.setTotalPrice(order.getTotalPrice().amount());
        entity.setStatus(order.getStatus());
        entity.setAppliedCouponCode(order.getAppliedCouponCode());
        return entity;
    }
}
