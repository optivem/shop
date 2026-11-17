package com.mycompany.myshop.backend.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * The {@link OrderPlaced} consumer: records one {@code order_placed_audit} row per event. The event id
 * is that table's primary key and the insert is {@code ON CONFLICT DO NOTHING}, so a redelivery is a
 * no-op: the idempotent half of at-least-once delivery.
 */
@Component
public class OrderPlacedAuditHandler implements OutboxEventHandler {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public OrderPlacedAuditHandler(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this(jdbc, objectMapper, Clock.systemUTC());
    }

    OrderPlacedAuditHandler(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String eventType() {
        return OrderPlaced.EVENT_TYPE;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OutboxEvent event) {
        var placed = read(event.payload());
        jdbc.update("INSERT INTO order_placed_audit (event_id, order_number, sku, quantity, total_price, placed_at, recorded_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING",
                event.id(), placed.orderNumber(), placed.sku(), placed.quantity(), placed.totalPrice(),
                placed.placedAt().atOffset(ZoneOffset.UTC), clock.instant().atOffset(ZoneOffset.UTC));
    }

    private OrderPlaced read(String payload) {
        try {
            return objectMapper.readValue(payload, OrderPlaced.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unreadable " + OrderPlaced.EVENT_TYPE + " payload", e);
        }
    }
}
