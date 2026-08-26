package com.mycompany.myshop.backend.usecases.commands.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        verify(orderRepository).update(order);
    }

    // Refused before the lookup, so the repository is never asked at all -- which is the point of
    // the policy being the first line of the use case.
    @Test
    void cancelOrderRefusesDuringDecember31CancellationBlackout() {
        when(clockGateway.getCurrentTime()).thenReturn(DEC_31_CANCEL_BLACKOUT);

        assertThatThrownBy(() -> cancelOrder.execute(new CancelOrderRequest("ORD-001")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("December 31");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void cancelOrderRefusesAnOrderThatIsAlreadyCancelled() {
        givenNormalTime();
        var order = orderWith(OrderStatus.CANCELLED);
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-001"))).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> cancelOrder.execute(new CancelOrderRequest("ORD-001")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already been cancelled");
        verify(orderRepository, never()).update(any());
    }

    // The disagreement that keeps OrderNumber.parse returning a Result: DeliverOrder answers a
    // malformed number with "malformed", this use case answers it with "no such order".
    @Test
    void cancelOrderReportsNotFoundWhenOrderNumberIsMalformed() {
        givenNormalTime();

        var result = cancelOrder.execute(new CancelOrderRequest(" "));

        assertThat(result.error()).isEqualTo(new UseCaseError.NotFound("Order", " "));
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
        return Order.restore(OrderNumber.of("ORD-001"), NORMAL_TIME, Country.of("US"), Sku.of("BOOK-123"),
                pricing, status, null);
    }
}
