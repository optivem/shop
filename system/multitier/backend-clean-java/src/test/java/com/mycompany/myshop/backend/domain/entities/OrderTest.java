package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderTest {

    private static final Instant PLACED_AT = Instant.parse("2025-06-15T10:00:00Z");

    @Test
    void deliverMovesAPlacedOrderToDelivered() {
        var order = orderWith(OrderStatus.PLACED);

        order.deliver();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void deliverRejectsAnOrderThatIsAlreadyDelivered() {
        var order = orderWith(OrderStatus.DELIVERED);

        var thrown = catchThrowable(order::deliver);

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("Order cannot be delivered in its current status");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void deliverRejectsACancelledOrder() {
        var order = orderWith(OrderStatus.CANCELLED);

        var thrown = catchThrowable(order::deliver);

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("Order cannot be delivered in its current status");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelMovesAPlacedOrderToCancelled() {
        var order = orderWith(OrderStatus.PLACED);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelMovesADeliveredOrderToCancelled() {
        var order = orderWith(OrderStatus.DELIVERED);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelRejectsAnOrderThatIsAlreadyCancelled() {
        var order = orderWith(OrderStatus.CANCELLED);

        var thrown = catchThrowable(order::cancel);

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("Order has already been cancelled");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void rejectsConstructionWithoutAnOrderNumber() {
        var thrown = catchThrowable(() -> new Order(null, PLACED_AT, Country.of("US"), "BOOK-123", pricing(),
                OrderStatus.PLACED, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderNumber cannot be null");
    }

    @Test
    void rejectsConstructionWithoutAPricing() {
        var thrown = catchThrowable(() -> new Order("ORD-001", PLACED_AT, Country.of("US"), "BOOK-123", null,
                OrderStatus.PLACED, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pricing cannot be null");
    }

    @Test
    void rejectsConstructionWithoutAStatus() {
        var thrown = catchThrowable(() -> new Order("ORD-001", PLACED_AT, Country.of("US"), "BOOK-123",
                pricing(), null, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("status cannot be null");
    }

    @Test
    void acceptsAnOrderPlacedWithoutACoupon() {
        var order = orderWith(OrderStatus.PLACED);

        assertThat(order.getAppliedCouponCode()).isNull();
    }

    @Test
    void exposesTheComponentsOfItsPricing() {
        var order = orderWith(OrderStatus.PLACED);

        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getUnitPrice()).isEqualTo(Money.of("10.00"));
        assertThat(order.getBasePrice()).isEqualTo(Money.of("20.00"));
        assertThat(order.getDiscountRate()).isEqualTo(Rate.ZERO);
        assertThat(order.getDiscountAmount()).isEqualTo(Money.ZERO);
        assertThat(order.getSubtotalPrice()).isEqualTo(Money.of("20.00"));
        assertThat(order.getTaxRate()).isEqualTo(Rate.of("0.10"));
        assertThat(order.getTaxAmount()).isEqualTo(Money.of("2.00"));
        assertThat(order.getTotalPrice()).isEqualTo(Money.of("22.00"));
    }

    private static Order orderWith(OrderStatus status) {
        return new Order("ORD-001", PLACED_AT, Country.of("US"), "BOOK-123", pricing(), status, null);
    }

    private static OrderPricing pricing() {
        return OrderPricing.price(Money.of("10.00"), 2, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
    }
}
