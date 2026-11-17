package com.mycompany.myshop.backend.core.outbox;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Delivers outbox events to their handlers, at least once.
 *
 * <p>One {@link #relayBatch} call is one transaction: lock a batch of due, unsent events
 * ({@code FOR UPDATE SKIP LOCKED}), hand each to its handler (which commits on its own), then mark it
 * sent. If the process dies after a handler committed but before the batch committed, the locks are
 * released, the events are still unsent, and the next poll delivers them again; the handler's
 * idempotency makes that second delivery a no-op. A handler that throws leaves its event unsent with
 * {@code attempts + 1} and a backed-off {@code next_attempt_at}; the rest of the batch carries on.
 *
 * <p>Each delivery is one Micrometer observation, {@code outbox.relay}: a span when tracing exports,
 * plus a timer tagged with the event type and outcome.
 */
@Component
public class OutboxRelay {

    static final String OBSERVATION_NAME = "outbox.relay";
    static final int MAX_ERROR_LENGTH = 1000;

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore store;
    private final Map<String, OutboxEventHandler> handlers;
    private final TransactionOperations transactions;
    private final ObservationRegistry observationRegistry;
    private final OutboxRelaySettings settings;
    private final Clock clock;

    @Autowired
    public OutboxRelay(OutboxStore store, List<OutboxEventHandler> handlers, PlatformTransactionManager transactionManager,
                       ObservationRegistry observationRegistry, OutboxRelaySettings settings) {
        this(store, handlers, new TransactionTemplate(transactionManager), observationRegistry, settings, Clock.systemUTC());
    }

    public OutboxRelay(OutboxStore store, List<OutboxEventHandler> handlers, TransactionOperations transactions,
                       ObservationRegistry observationRegistry, OutboxRelaySettings settings, Clock clock) {
        this.store = store;
        this.handlers = handlers.stream().collect(Collectors.toMap(OutboxEventHandler::eventType, Function.identity()));
        this.transactions = transactions;
        this.observationRegistry = observationRegistry;
        this.settings = settings;
        this.clock = clock;
    }

    /** Relays one batch of due events. Returns how many were claimed (delivered or failed). */
    public int relayBatch() {
        var claimed = transactions.execute(status -> {
            var batch = store.claimBatch(settings.batchSize(), settings.maxAttempts(), clock.instant());
            batch.forEach(this::relay);
            return batch.size();
        });
        return claimed == null ? 0 : claimed;
    }

    private void relay(OutboxEvent event) {
        var observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
                .contextualName("outbox relay " + event.eventType())
                .lowCardinalityKeyValue("event.type", event.eventType())
                .highCardinalityKeyValue("event.id", event.id().toString())
                .highCardinalityKeyValue("aggregate.id", event.aggregateId())
                .start();
        try (var scope = observation.openScope()) {
            handlerFor(event).handle(event);
            store.markSent(event.id(), clock.instant());
            observation.lowCardinalityKeyValue("outcome", "sent");
        } catch (RuntimeException e) {
            observation.lowCardinalityKeyValue("outcome", "failed");
            observation.error(e);
            recordFailure(event, e);
        } finally {
            observation.stop();
        }
    }

    private OutboxEventHandler handlerFor(OutboxEvent event) {
        var handler = handlers.get(event.eventType());
        if (handler == null) {
            throw new IllegalStateException("No outbox handler for event type " + event.eventType());
        }
        return handler;
    }

    private void recordFailure(OutboxEvent event, RuntimeException e) {
        var attempts = event.attempts() + 1;
        store.markFailed(event.id(), attempts, clock.instant().plus(settings.backoff(attempts)), describe(e));
        if (attempts >= settings.maxAttempts()) {
            log.error("Outbox event {} ({}) failed {} times and will not be retried", event.id(), event.eventType(), attempts, e);
        } else {
            log.warn("Outbox event {} ({}) failed (attempt {} of {}); will retry", event.id(), event.eventType(), attempts,
                    settings.maxAttempts(), e);
        }
    }

    private static String describe(RuntimeException e) {
        var text = e.getClass().getName() + ": " + e.getMessage();
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }
}
