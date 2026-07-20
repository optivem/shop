package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.AbstractIntegrationTest;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

// No ERP stub -> no DSL refactor -> no latest/legacy twin. This test drives the real database
// through JPA and stubs nothing at all.
//
// @DataJpaTest rather than @SpringBootTest: layer 2 is one adapter, so booting the whole context
// would let an unrelated bean (e.g. an ERP gateway misconfiguration) redden a repository test.
// replace = NONE keeps the Testcontainers Postgres from AbstractIntegrationTest instead of
// substituting an embedded database -- the schema is Flyway-built and ddl-auto is `validate`,
// so this test is only meaningful against real Postgres.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

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
        forceDatabaseRoundTrip();

        var found = orderRepository.findByOrderNumber("ORD-001");
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("BOOK-123");
        assertThat(found.get().getTotalPrice()).isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    /**
     * Pushes pending writes to the database and detaches everything, so the next read is a real
     * round trip. Needed because {@code @DataJpaTest} wraps each test in a transaction: without it a
     * read-back is served from Hibernate's first-level cache and hands back the very object just
     * saved, passing even if the entity-to-table mapping were broken.
     *
     * <p>Only round-trip tests (write, then read back through the repository) need this. A read-only
     * test over seeded data has nothing pending to flush.
     */
    private void forceDatabaseRoundTrip() {
        entityManager.flush();
        entityManager.clear();
    }
}
