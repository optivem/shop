package com.mycompany.myshop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.AbstractIntegrationTest;
import com.mycompany.myshop.core.entities.Order;
import com.mycompany.myshop.core.entities.OrderStatus;
import com.mycompany.myshop.core.repositories.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void clearOrders() {
        orderRepository.deleteAll();
    }

    @Test
    void savesAndReadsBackOrder() {
        Order order = new Order(
            "ORD-001",
            Instant.parse("2026-01-01T00:00:00Z"),
            "US",
            "BOOK-123",
            2,
            new BigDecimal("10.00"),
            new BigDecimal("20.00"),
            new BigDecimal("0.0000"),
            new BigDecimal("0.00"),
            new BigDecimal("20.00"),
            new BigDecimal("0.1000"),
            new BigDecimal("2.00"),
            new BigDecimal("22.00"),
            OrderStatus.PLACED,
            null,
            "customer-sub",
            "customer1"
        );

        orderRepository.save(order);

        Optional<Order> found = orderRepository.findByOrderNumber("ORD-001");
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("BOOK-123");
        assertThat(found.get().getTotalPrice()).isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.get().getOwner()).isEqualTo("customer-sub");
        assertThat(found.get().getOwnerName()).isEqualTo("customer1");
    }
}
