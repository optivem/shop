package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderTest {

    private static final Instant PLACED_AT = Instant.parse("2025-06-15T10:00:00Z");

    @Test
    void placeStartsAnOrderInThePlacedStatus() {
        var order = Order.place(OrderNumber.of("ORD-001"), PLACED_AT, Country.of("US"),
                Sku.of("BOOK-123"), pricing(), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void deliverMovesAPlacedOrderToDelivered() {
        var order = orderWith(OrderStatus.PLACED);

        var result = order.deliver();

        assertThat(result.isOk()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void deliverRejectsAnOrderThatIsAlreadyDelivered() {
        var order = orderWith(OrderStatus.DELIVERED);

        var result = order.deliver();

        assertThat(result.isOk()).isFalse();
        assertThat(result.error().message()).isEqualTo("Order cannot be delivered in its current status");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void deliverRejectsACancelledOrder() {
        var order = orderWith(OrderStatus.CANCELLED);

        var result = order.deliver();

        assertThat(result.isOk()).isFalse();
        assertThat(result.error().message()).isEqualTo("Order cannot be delivered in its current status");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelMovesAPlacedOrderToCancelled() {
        var order = orderWith(OrderStatus.PLACED);

        var outcome = order.cancel();

        assertThat(outcome).isInstanceOf(CancelOutcome.Cancelled.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelRejectsADeliveredOrder() {
        var order = orderWith(OrderStatus.DELIVERED);

        var outcome = order.cancel();

        assertThat(outcome).isInstanceOfSatisfying(CancelOutcome.NotCancellable.class, notCancellable ->
                assertThat(notCancellable.violation().message())
                        .isEqualTo("Order cannot be cancelled in its current status"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancelRejectsAnOrderThatIsAlreadyCancelled() {
        var order = orderWith(OrderStatus.CANCELLED);

        var outcome = order.cancel();

        assertThat(outcome).isInstanceOfSatisfying(CancelOutcome.AlreadyCancelled.class, alreadyCancelled ->
                assertThat(alreadyCancelled.violation().message())
                        .isEqualTo("Order has already been cancelled"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void rejectsConstructionWithoutAnOrderNumber() {
        var thrown = catchThrowable(() -> Order.restore(null, PLACED_AT, Country.of("US"), Sku.of("BOOK-123"), pricing(),
                OrderStatus.PLACED, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderNumber cannot be null");
    }

    @Test
    void rejectsConstructionWithoutAPricing() {
        var thrown = catchThrowable(() -> Order.restore(OrderNumber.of("ORD-001"), PLACED_AT, Country.of("US"), Sku.of("BOOK-123"), null,
                OrderStatus.PLACED, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pricing cannot be null");
    }

    @Test
    void rejectsConstructionWithoutAStatus() {
        var thrown = catchThrowable(() -> Order.restore(OrderNumber.of("ORD-001"), PLACED_AT, Country.of("US"), Sku.of("BOOK-123"),
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
        var pricing = orderWith(OrderStatus.PLACED).getPricing();

        assertThat(pricing.quantity()).isEqualTo(2);
        assertThat(pricing.unitPrice()).isEqualTo(Money.of("10.00"));
        assertThat(pricing.basePrice()).isEqualTo(Money.of("20.00"));
        assertThat(pricing.discountRate()).isEqualTo(Rate.ZERO);
        assertThat(pricing.discountAmount()).isEqualTo(Money.ZERO);
        assertThat(pricing.subtotalPrice()).isEqualTo(Money.of("20.00"));
        assertThat(pricing.taxRate()).isEqualTo(Rate.of("0.10"));
        assertThat(pricing.taxAmount()).isEqualTo(Money.of("2.00"));
        assertThat(pricing.totalPrice()).isEqualTo(Money.of("22.00"));
    }

    private static Order orderWith(OrderStatus status) {
        return Order.restore(OrderNumber.of("ORD-001"), PLACED_AT, Country.of("US"), Sku.of("BOOK-123"),
                pricing(), status, null);
    }

    private static OrderPricing pricing() {
        return OrderPricing.price(Money.of("10.00"), 2, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
    }
}
