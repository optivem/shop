package com.mycompany.myshop.backend.core.outbox;

/**
 * Consumes one event type from the outbox. The relay delivers at least once, so {@link #handle} must
 * be idempotent on {@link OutboxEvent#id()}: a redelivered event has to change nothing.
 *
 * <p>{@link #handle} must run in its own transaction ({@code REQUIRES_NEW}), the way a real consumer
 * on the far side of a broker would: its work commits independently of the relay marking the event
 * sent, and a failure inside it cannot poison the relay's transaction.
 */
public interface OutboxEventHandler {

    String eventType();

    void handle(OutboxEvent event);
}
