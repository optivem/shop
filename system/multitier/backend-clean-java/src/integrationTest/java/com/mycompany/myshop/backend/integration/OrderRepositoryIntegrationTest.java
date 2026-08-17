package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.infrastructure.persistence.adapters.OrderRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// No ERP stub -> no DSL refactor -> no latest/legacy twin. This test drives the real database
// through JPA and stubs nothing at all.
//
// The subject is the whole persistence adapter stack the clean-architecture refactor introduced:
// the domain POJO goes in through the OrderRepository port, OrderMapper turns it into an
// OrderJpaEntity, Hibernate writes it to real Postgres, and the read-back comes all the way home as
// a domain Order again. That mapping is where the domain's Money/Rate meet the columns' plain
// DECIMAL, and it has no other test -- backend-java's equivalent had no mapping to get wrong.
//
// @DataJpaTest rather than @SpringBootTest: layer 2 is one adapter, so booting the whole context
// would let an unrelated bean (e.g. an ERP gateway misconfiguration) redden a repository test.
// @DataJpaTest scans JPA repositories and entities but not @Component, so the adapter under test is
// registered explicitly by @Import.
// replace = NONE keeps the Testcontainers Postgres supplied by @Import(TestcontainersConfiguration)
// instead of substituting an embedded database -- the schema is Flyway-built and ddl-auto is
// `validate`, so this test is only meaningful against real Postgres.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, OrderRepositoryAdapter.class})
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsBackOrder() {
        var pricing = new OrderPricing(
            Money.of("10.00"), 2, Money.of("20.00"),
            Rate.ZERO, Money.ZERO, Money.of("20.00"),
            Rate.of("0.1000"), Money.of("2.00"), Money.of("22.00"));

        var order = new Order(
            "ORD-001",
            Instant.parse("2026-01-01T00:00:00Z"),
            "US",
            "BOOK-123",
            pricing,
            OrderStatus.PLACED,
            null
        );

        orderRepository.save(order);
        forceDatabaseRoundTrip();

        var found = orderRepository.findByOrderNumber("ORD-001");
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("BOOK-123");
        assertThat(found.get().getTotalPrice()).isEqualTo(Money.of("22.00"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    /**
     * Every priced field survives the round trip, not just the total. The mapper writes nine
     * {@code Money}/{@code Rate} values into plain {@code DECIMAL} columns and reads them back, so a
     * transposed pair (say {@code discountAmount} into the {@code taxAmount} column) would leave the
     * total correct and the breakdown wrong — which the assertion above alone would not catch.
     */
    @Test
    void savesAndReadsBackEveryPricedField() {
        var pricing = new OrderPricing(
            Money.of("10.00"), 3, Money.of("30.00"),
            Rate.of("0.1000"), Money.of("3.00"), Money.of("27.00"),
            Rate.of("0.2000"), Money.of("5.40"), Money.of("32.40"));

        orderRepository.save(new Order(
            "ORD-002",
            Instant.parse("2026-01-02T00:00:00Z"),
            "DE",
            "BOOK-456",
            pricing,
            OrderStatus.PLACED,
            CouponCode.of("SAVE10")
        ));
        forceDatabaseRoundTrip();

        var found = orderRepository.findByOrderNumber("ORD-002");
        assertThat(found).isPresent();
        assertThat(found.get().getPricing()).isEqualTo(pricing);
        assertThat(found.get().getCountry()).isEqualTo("DE");
        assertThat(found.get().getAppliedCouponCode()).isEqualTo(CouponCode.of("SAVE10"));
        assertThat(found.get().getOrderTimestamp())
            .isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
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
