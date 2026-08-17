package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        cancelOrder.execute("ORD-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderThrowsDuringDecember31CancellationBlackout() {
        when(clockGateway.getCurrentTime()).thenReturn(DEC_31_CANCEL_BLACKOUT);

        var thrown = catchThrowable(() -> cancelOrder.execute("ORD-001"));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("December 31");
    }

    @Test
    void cancelOrderThrowsWhenOrderAlreadyCancelled() {
        givenNormalTime();
        var order = orderWith(OrderStatus.CANCELLED);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var thrown = catchThrowable(() -> cancelOrder.execute("ORD-001"));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("already been cancelled");
    }

    private void givenNormalTime() {
        when(clockGateway.getCurrentTime()).thenReturn(NORMAL_TIME);
    }

    private Order orderWith(OrderStatus status) {
        var pricing = OrderPricing.price(Money.of("10.00"), 1, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
        return new Order("ORD-001", NORMAL_TIME, Country.of("US"), "BOOK-123", pricing, status, null);
    }
}
