package com.mycompany.myshop.backend.usecases.commands.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverOrderTest {

    private static final Instant NORMAL_TIME = Instant.parse("2025-06-15T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DeliverOrder deliverOrder;

    @Test
    void deliverOrderTransitionsStatusToDelivered() {
        var order = orderWith(OrderStatus.PLACED);
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-001"))).thenReturn(Optional.of(order));

        var result = deliverOrder.execute(new DeliverOrderRequest("ORD-001"));

        assertThat(result.isOk()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).update(order);
    }

    @Test
    void deliverOrderReportsNotFoundWhenOrderDoesNotExist() {
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-999"))).thenReturn(Optional.empty());

        var result = deliverOrder.execute(new DeliverOrderRequest("ORD-999"));

        assertThat(result.error()).isEqualTo(new UseCaseError.NotFound("Order", "ORD-999"));
    }

    // The refusal leaves as an exception, because this use case no longer catches one: turning it
    // into the 422 the caller sees is RefusalTranslatingUseCase's job, and is tested there. What is
    // this use case's job, and is tested here, is that the refusal stops the write.
    @Test
    void deliverOrderRefusesAnOrderThatIsAlreadyDelivered() {
        var order = orderWith(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber(OrderNumber.of("ORD-001"))).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> deliverOrder.execute(new DeliverOrderRequest("ORD-001")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be delivered");
        verify(orderRepository, never()).update(any());
    }

    @Test
    void deliverOrderReportsInvalidWhenOrderNumberIsMalformed() {
        var result = deliverOrder.execute(new DeliverOrderRequest(" "));

        assertThat(result.error()).isEqualTo(
                new UseCaseError.Invalid("orderNumber", "Order number must not be empty"));
    }

    private Order orderWith(OrderStatus status) {
        var pricing = OrderPricing.price(Money.of("10.00"), 1, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
        return Order.restore(OrderNumber.of("ORD-001"), NORMAL_TIME, Country.of("US"), Sku.of("BOOK-123"),
                pricing, status, null);
    }
}
