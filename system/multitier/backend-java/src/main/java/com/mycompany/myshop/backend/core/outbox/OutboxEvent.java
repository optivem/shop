package com.mycompany.myshop.backend.core.outbox;

import java.util.UUID;

/**
 * One outbox row as the relay hands it to a handler. {@code id} is the event id (consumers deduplicate
 * on it); {@code payload} is the event body as JSON; {@code attempts} counts the failed deliveries so far.
 */
public record OutboxEvent(UUID id, String aggregateType, String aggregateId, String eventType, String payload, int attempts) {
}
