package com.mycompany.myshop.backend.core.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Bound from {@code outbox.relay.*}; every value has a default, so nothing needs to be configured.
 * {@code enabled=false} (env {@code OUTBOX_RELAY_ENABLED=false}) switches the polling off: orders still
 * write their events, they just stay unsent until the relay is switched back on.
 *
 * <p>A failed delivery is retried after {@code initialBackoff * backoffMultiplier^(attempts - 1)};
 * after {@code maxAttempts} failures the event stays unsent and is no longer polled.
 */
@ConfigurationProperties(prefix = "outbox.relay")
public record OutboxRelaySettings(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("2s") Duration pollInterval,
        @DefaultValue("50") int batchSize,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("1s") Duration initialBackoff,
        @DefaultValue("2.0") double backoffMultiplier) {

    /** The delay before the next try, once an event has failed {@code attempts} times (1 or more). */
    public Duration backoff(int attempts) {
        var factor = Math.pow(backoffMultiplier, Math.max(0, attempts - 1));
        return Duration.ofMillis(Math.round(initialBackoff.toMillis() * factor));
    }
}
