package com.mycompany.myshop.backend.component.outbox;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.config.OutboxConfig;
import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.outbox.OrderPlaced;
import com.mycompany.myshop.backend.core.outbox.OrderPlacedAuditHandler;
import com.mycompany.myshop.backend.core.outbox.OutboxEvent;
import com.mycompany.myshop.backend.core.outbox.OutboxEventHandler;
import com.mycompany.myshop.backend.core.outbox.OutboxRelay;
import com.mycompany.myshop.backend.core.outbox.OutboxRelaySettings;
import com.mycompany.myshop.backend.core.outbox.OutboxStore;
import com.mycompany.myshop.backend.core.services.OrderService;
import com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The transactional outbox against real Postgres: the OrderPlaced row is written atomically with the
 * order, and the relay delivers it at least once to an idempotent consumer. The scheduled relay is
 * switched off for this layer ({@code outbox.relay.enabled=false} in application-test.yml), so each
 * test drives {@link OutboxRelay#relayBatch} itself and nothing races it.
 */
class TransactionalOutboxComponentTest extends BaseComponentTest {

    private static final String SKU = "OUTBOX-BOOK";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxStore outboxStore;

    @Autowired
    private OrderPlacedAuditHandler auditHandler;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void clearOutbox() {
        // Other component tests place orders too; their events are irrelevant here.
        jdbc.update("DELETE FROM outbox");
        jdbc.update("DELETE FROM order_placed_audit");
    }

    @Test
    void placingAnOrderWritesOneUnsentOrderPlacedEvent() {
        placeOrder();

        var rows = outboxRows();
        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.get("aggregate_type")).isEqualTo(OrderPlaced.AGGREGATE_TYPE);
        assertThat(row.get("event_type")).isEqualTo(OrderPlaced.EVENT_TYPE);
        assertThat(row.get("sent_at")).isNull();
        assertThat(row.get("attempts")).isEqualTo(0);

        var orderNumber = (String) row.get("aggregate_id");
        assertThat(orderRepository.findByOrderNumber(orderNumber)).isPresent();
        var payload = jdbc.queryForMap("SELECT payload->>'orderNumber' AS order_number, payload->>'sku' AS sku, "
                + "(payload->>'quantity')::int AS quantity, payload->>'totalPrice' AS total_price FROM outbox");
        assertThat(payload.get("order_number")).isEqualTo(orderNumber);
        assertThat(payload.get("sku")).isEqualTo(SKU);
        assertThat(payload.get("quantity")).isEqualTo(2);
        assertThat(payload.get("total_price")).isEqualTo("44.00");
    }

    @Test
    void rolledBackPlaceOrderLeavesNeitherTheOrderNorTheEvent() {
        placeOrder();  // stubs the externals, and leaves one committed order + event to compare against
        var customer = new CurrentUser("customer-user", "customer-user", false);
        var request = new PlaceOrderRequest();
        request.setSku(SKU);
        request.setQuantity(1);
        request.setCountry("US");

        var orderNumber = new TransactionTemplate(transactionManager).execute(status -> {
            var response = orderService.placeOrder(request, customer);
            orderRepository.flush();  // both inserts have reached Postgres...
            assertThat(countOutboxRowsFor(response.getOrderNumber())).isEqualTo(1);
            status.setRollbackOnly();  // ...and now the transaction fails after the outbox write
            return response.getOrderNumber();
        });

        assertThat(orderRepository.findByOrderNumber(orderNumber)).isEmpty();
        assertThat(countOutboxRowsFor(orderNumber)).isZero();
        assertThat(outboxRows()).hasSize(1);
    }

    @Test
    void relayDeliversThePendingEventToTheConsumerAndMarksItSent() {
        placeOrder();
        var orderNumber = (String) outboxRows().get(0).get("aggregate_id");

        assertThat(relay.relayBatch()).isEqualTo(1);

        var audit = jdbc.queryForMap("SELECT order_number, sku, quantity, total_price FROM order_placed_audit");
        assertThat(audit.get("order_number")).isEqualTo(orderNumber);
        assertThat(audit.get("sku")).isEqualTo(SKU);
        assertThat(audit.get("quantity")).isEqualTo(2);
        assertThat((BigDecimal) audit.get("total_price")).isEqualByComparingTo("44.00");
        assertThat(outboxRows().get(0).get("sent_at")).isNotNull();
        assertThat(relay.relayBatch()).isZero();
    }

    @Test
    void deliveryWhoseAcknowledgementIsLostIsRedeliveredAndHandledOnce() {
        placeOrder();
        // The consumer commits, then the relay "crashes" before it can mark the event sent.
        var crashAfterDelivery = new OutboxEventHandler() {
            @Override
            public String eventType() {
                return OrderPlaced.EVENT_TYPE;
            }

            @Override
            public void handle(OutboxEvent event) {
                auditHandler.handle(event);
                throw new IllegalStateException("relay crashed before mark-sent");
            }
        };

        relayWith(crashAfterDelivery).relayBatch();

        assertThat(countAuditRows()).isEqualTo(1);
        assertThat(outboxRows().get(0).get("sent_at")).isNull();
        assertThat(outboxRows().get(0).get("attempts")).isEqualTo(1);

        assertThat(relayWith(auditHandler).relayBatch()).isEqualTo(1);

        assertThat(countAuditRows()).isEqualTo(1);
        assertThat(outboxRows().get(0).get("sent_at")).isNotNull();
    }

    @Test
    void pendingGaugeReportsTheUnsentBacklog() {
        placeOrder();
        placeOrder();
        assertThat(pendingGauge()).isEqualTo(2.0);

        relay.relayBatch();

        assertThat(pendingGauge()).isZero();
    }

    private void placeOrder() {
        scenario.given()
                .product().withSku(SKU).withUnitPrice("20.00")
            .and().country().withCode("US").withTaxRate("0.10")
            .when().placeOrder()
                .withSku(SKU)
                .withQuantity(2)
                .withCountry("US")
            .then().shouldSucceed();
        // One scenario per instance: start a fresh one for the next order in the same test.
        scenario = new ScenarioDslImpl(app);
    }

    /** A relay over the real store, with no backoff so a retry is due at once. */
    private OutboxRelay relayWith(OutboxEventHandler handler) {
        var settings = new OutboxRelaySettings(true, Duration.ofSeconds(2), 50, 5, Duration.ZERO, 2.0);
        return new OutboxRelay(outboxStore, List.of(handler), new TransactionTemplate(transactionManager),
                ObservationRegistry.NOOP, settings, Clock.systemUTC());
    }

    private List<Map<String, Object>> outboxRows() {
        return jdbc.queryForList("SELECT aggregate_type, aggregate_id, event_type, sent_at, attempts FROM outbox ORDER BY created_at");
    }

    private long countOutboxRowsFor(String orderNumber) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox WHERE aggregate_id = ?", Long.class, orderNumber);
    }

    private long countAuditRows() {
        return jdbc.queryForObject("SELECT count(*) FROM order_placed_audit", Long.class);
    }

    private double pendingGauge() {
        return meterRegistry.get(OutboxConfig.PENDING_GAUGE).gauge().value();
    }
}
