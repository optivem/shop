package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
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
class DeliverOrderTest {

    private static final Instant NORMAL_TIME = Instant.parse("2025-06-15T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DeliverOrder deliverOrder;

    @Test
    void deliverOrderTransitionsStatusToDelivered() {
        var order = orderWith(OrderStatus.PLACED);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        deliverOrder.execute("ORD-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).save(order);
    }

    @Test
    void deliverOrderThrowsWhenOrderNotFound() {
        when(orderRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

        var thrown = catchThrowable(() -> deliverOrder.execute("ORD-999"));

        assertThat(thrown).isInstanceOf(NotExistValidationException.class);
    }

    @Test
    void deliverOrderThrowsWhenOrderAlreadyDelivered() {
        var order = orderWith(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var thrown = catchThrowable(() -> deliverOrder.execute("ORD-001"));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be delivered");
    }

    private Order orderWith(OrderStatus status) {
        var pricing = OrderPricing.price(Money.of("10.00"), 1, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
        return new Order("ORD-001", NORMAL_TIME, Country.of("US"), "BOOK-123", pricing, status, null);
    }
}
