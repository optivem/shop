package com.mycompany.myshop.backend.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * The {@code outbox} table, over plain JDBC. {@link JdbcTemplate} joins the JPA transaction that is
 * already open (same connection), which is the whole point: {@link #append} commits or rolls back
 * together with the order it was written for.
 */
@Repository
public class OutboxStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public OutboxStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this(jdbc, objectMapper, Clock.systemUTC());
    }

    OutboxStore(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Writes one event inside the caller's transaction; there must be one. Returns the event id. */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID append(String aggregateType, String aggregateId, String eventType, Object payload) {
        var id = UUID.randomUUID();
        var now = utc(clock.instant());
        jdbc.update("INSERT INTO outbox (id, aggregate_type, aggregate_id, event_type, payload, created_at, attempts, next_attempt_at) "
                        + "VALUES (?, ?, ?, ?, ?::jsonb, ?, 0, ?)",
                id, aggregateType, aggregateId, eventType, toJson(payload), now, now);
        return id;
    }

    /**
     * Locks up to {@code limit} unsent events that are due, oldest first, for the caller's transaction.
     * {@code SKIP LOCKED} lets several relays (one per instance) poll at once without taking the same
     * row twice. Events that already failed {@code maxAttempts} times are left alone.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<OutboxEvent> claimBatch(int limit, int maxAttempts, Instant now) {
        return jdbc.query("SELECT id, aggregate_type, aggregate_id, event_type, payload::text AS payload, attempts FROM outbox "
                        + "WHERE sent_at IS NULL AND attempts < ? AND next_attempt_at <= ? "
                        + "ORDER BY created_at, id LIMIT ? FOR UPDATE SKIP LOCKED",
                (rs, rowNum) -> new OutboxEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getInt("attempts")),
                maxAttempts, utc(now), limit);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markSent(UUID id, Instant sentAt) {
        jdbc.update("UPDATE outbox SET sent_at = ? WHERE id = ?", utc(sentAt), id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markFailed(UUID id, int attempts, Instant nextAttemptAt, String error) {
        jdbc.update("UPDATE outbox SET attempts = ?, next_attempt_at = ?, last_error = ? WHERE id = ?",
                attempts, utc(nextAttemptAt), error, id);
    }

    /** Unsent events, including the ones that ran out of attempts: the backlog an operator watches. */
    public long countPending() {
        var count = jdbc.queryForObject("SELECT count(*) FROM outbox WHERE sent_at IS NULL", Long.class);
        return count == null ? 0 : count;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the outbox payload", e);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
