package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.AbstractIntegrationTest;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// No ERP stub -> no DSL refactor -> no latest/legacy twin. This test drives the real database
// through JPA and stubs nothing at all.
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
        var order = new Order(
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
            null
        );

        orderRepository.save(order);

        var found = orderRepository.findByOrderNumber("ORD-001");
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("BOOK-123");
        assertThat(found.get().getTotalPrice()).isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
    }
}
