package com.mycompany.myshop.backend.infrastructure.persistence.mappers;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
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
                OrderNumber.of(entity.getOrderNumber()),
                entity.getOrderTimestamp(),
                Country.of(entity.getCountry()),
                Sku.of(entity.getSku()),
                pricing,
                entity.getStatus(),
                // Nullable column: an order placed without a coupon has none.
                CouponCode.requested(entity.getAppliedCouponCode()).orElse(null));
    }

    public static OrderJpaEntity toEntity(Order order) {
        var pricing = order.getPricing();

        var entity = new OrderJpaEntity();
        entity.setOrderNumber(order.getOrderNumber().value());
        entity.setOrderTimestamp(order.getOrderTimestamp());
        entity.setCountry(order.getCountry().value());
        entity.setSku(order.getSku().value());
        entity.setQuantity(pricing.quantity());
        entity.setUnitPrice(pricing.unitPrice().amount());
        entity.setBasePrice(pricing.basePrice().amount());
        entity.setDiscountRate(pricing.discountRate().value());
        entity.setDiscountAmount(pricing.discountAmount().amount());
        entity.setSubtotalPrice(pricing.subtotalPrice().amount());
        entity.setTaxRate(pricing.taxRate().value());
        entity.setTaxAmount(pricing.taxAmount().amount());
        entity.setTotalPrice(pricing.totalPrice().amount());
        entity.setStatus(order.getStatus());
        var appliedCouponCode = order.getAppliedCouponCode();
        entity.setAppliedCouponCode(appliedCouponCode == null ? null : appliedCouponCode.value());
        return entity;
    }
}
