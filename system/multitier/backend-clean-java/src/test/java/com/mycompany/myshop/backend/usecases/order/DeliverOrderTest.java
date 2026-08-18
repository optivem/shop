package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
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

        var result = deliverOrder.execute(new DeliverOrderRequest("ORD-001"));

        assertThat(result.isOk()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).save(order);
    }

    @Test
    void deliverOrderReportsNotFoundWhenOrderDoesNotExist() {
        when(orderRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

        var result = deliverOrder.execute(new DeliverOrderRequest("ORD-999"));

        assertThat(result.error()).isEqualTo(new UseCaseError.NotFound("Order", "ORD-999"));
    }

    @Test
    void deliverOrderReportsInvalidWhenOrderAlreadyDelivered() {
        var order = orderWith(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        var result = deliverOrder.execute(new DeliverOrderRequest("ORD-001"));

        assertThat(result.error()).isInstanceOfSatisfying(UseCaseError.Invalid.class,
                invalid -> assertThat(invalid.message()).contains("cannot be delivered"));
    }

    private Order orderWith(OrderStatus status) {
        var pricing = OrderPricing.price(Money.of("10.00"), 1, Rate.ONE, Rate.ZERO, Rate.of("0.10"));
        return new Order("ORD-001", NORMAL_TIME, Country.of("US"), "BOOK-123", pricing, status, null);
    }
}
