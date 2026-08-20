package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderTest {

    private static final Instant NORMAL_TIME = Instant.parse("2025-06-15T10:00:00Z");
    private static final Instant DEC_31_CANCEL_BLACKOUT = Instant.parse("2025-12-31T22:15:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ClockGateway clockGateway;

    @InjectMocks
    private CancelOrder cancelOrder;

    @Test
    void cancelOrderTransitionsStatusToCancelled() {
        givenNormalTime();
        var order = orderWith(OrderStatus.PLACED);
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-001"))).thenReturn(Optional.of(order));

        var result = cancelOrder.execute(new CancelOrderRequest("ORD-001"));

        assertThat(result.isOk()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderReportsInvalidDuringDecember31CancellationBlackout() {
        when(clockGateway.getCurrentTime()).thenReturn(DEC_31_CANCEL_BLACKOUT);

        var result = cancelOrder.execute(new CancelOrderRequest("ORD-001"));

        assertThat(result.error()).isInstanceOfSatisfying(UseCaseError.Invalid.class,
                invalid -> assertThat(invalid.message()).contains("December 31"));
    }

    @Test
    void cancelOrderReportsInvalidWhenOrderAlreadyCancelled() {
        givenNormalTime();
        var order = orderWith(OrderStatus.CANCELLED);
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-001"))).thenReturn(Optional.of(order));

        var result = cancelOrder.execute(new CancelOrderRequest("ORD-001"));

        assertThat(result.error()).isInstanceOfSatisfying(UseCaseError.Invalid.class,
                invalid -> assertThat(invalid.message()).contains("already been cancelled"));
    }

    @Test
    void cancelOrderReportsNotFoundWhenOrderDoesNotExist() {
        givenNormalTime();
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-999"))).thenReturn(Optional.empty());

        var result = cancelOrder.execute(new CancelOrderRequest("ORD-999"));

        assertThat(result.error()).isEqualTo(new UseCaseError.NotFound("Order", "ORD-999"));
    }

    private void givenNormalTime() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
    }

    private Order orderWith(OrderStatus status) {
        var pricing = OrderPricing.price(Money.of("10.00"), 1, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
        return new Order(OrderNumber.of("ORD-001"), NORMAL_TIME, Country.of("US"), Sku.of("BOOK-123"),
                pricing, status, null);
    }
}
