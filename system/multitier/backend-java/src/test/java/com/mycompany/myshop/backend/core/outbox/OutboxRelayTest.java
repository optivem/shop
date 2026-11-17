package com.mycompany.myshop.backend.core.outbox;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");
    private static final OutboxRelaySettings SETTINGS =
            new OutboxRelaySettings(true, Duration.ofSeconds(2), 50, 5, Duration.ofSeconds(1), 2.0);

    @Mock
    private OutboxStore store;

    private final FakeHandler handler = new FakeHandler();
    private final List<Observation.Context> observed = new ArrayList<>();
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        var observations = ObservationRegistry.create();
        observations.observationConfig().observationHandler(new RecordingHandler(observed));
        relay = new OutboxRelay(store, List.of(handler), TransactionOperations.withoutTransaction(), observations,
                SETTINGS, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void deliversEachClaimedEventThenMarksItSent() {
        var first = event(OrderPlaced.EVENT_TYPE, 0);
        var second = event(OrderPlaced.EVENT_TYPE, 0);
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of(first, second));

        var claimed = relay.relayBatch();

        assertThat(claimed).isEqualTo(2);
        assertThat(handler.handled).containsExactly(first, second);
        InOrder order = inOrder(store);
        order.verify(store).markSent(first.id(), NOW);
        order.verify(store).markSent(second.id(), NOW);
    }

    @Test
    void emptyBatchDeliversNothing() {
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of());

        assertThat(relay.relayBatch()).isZero();
        assertThat(handler.handled).isEmpty();
    }

    @Test
    void failedDeliveryStaysUnsentWithOneMoreAttemptAndABackedOffRetry() {
        var failing = event(OrderPlaced.EVENT_TYPE, 2);
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of(failing));
        handler.failWith(failing, new IllegalStateException("consumer down"));

        relay.relayBatch();

        // Third failure: 1s * 2^(3-1) = 4s.
        verify(store).markFailed(eq(failing.id()), eq(3), eq(NOW.plusSeconds(4)), startsWith("java.lang.IllegalStateException: consumer down"));
        verify(store, never()).markSent(any(), any());
    }

    @Test
    void oneFailedDeliveryDoesNotStopTheRestOfTheBatch() {
        var failing = event(OrderPlaced.EVENT_TYPE, 0);
        var healthy = event(OrderPlaced.EVENT_TYPE, 0);
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of(failing, healthy));
        handler.failWith(failing, new IllegalStateException("boom"));

        relay.relayBatch();

        verify(store).markFailed(eq(failing.id()), eq(1), eq(NOW.plusSeconds(1)), any());
        verify(store).markSent(healthy.id(), NOW);
    }

    @Test
    void eventWithoutAHandlerIsRecordedAsFailed() {
        var unknown = event("SomethingElse", 0);
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of(unknown));

        relay.relayBatch();

        verify(store).markFailed(eq(unknown.id()), eq(1), any(), eq("java.lang.IllegalStateException: No outbox handler for event type SomethingElse"));
        verify(store, never()).markSent(any(), any());
    }

    @Test
    void longErrorMessagesAreTruncatedToTheColumnBudget() {
        var failing = event(OrderPlaced.EVENT_TYPE, 0);
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of(failing));
        handler.failWith(failing, new IllegalStateException("x".repeat(5000)));

        relay.relayBatch();

        verify(store).markFailed(eq(failing.id()), anyInt(), any(),
                argThat(text -> text.length() == OutboxRelay.MAX_ERROR_LENGTH));
    }

    @Test
    void eachDeliveryIsOneObservationTaggedWithTypeAndOutcome() {
        var sent = event(OrderPlaced.EVENT_TYPE, 0);
        var failed = event(OrderPlaced.EVENT_TYPE, 0);
        when(store.claimBatch(50, 5, NOW)).thenReturn(List.of(sent, failed));
        handler.failWith(failed, new IllegalStateException("boom"));

        relay.relayBatch();

        assertThat(observed).hasSize(2);
        assertThat(observed).allSatisfy(context -> {
            assertThat(context.getName()).isEqualTo(OutboxRelay.OBSERVATION_NAME);
            assertThat(context.getLowCardinalityKeyValue("event.type").getValue()).isEqualTo(OrderPlaced.EVENT_TYPE);
        });
        assertThat(observed.get(0).getLowCardinalityKeyValue("outcome").getValue()).isEqualTo("sent");
        assertThat(observed.get(0).getHighCardinalityKeyValue("event.id").getValue()).isEqualTo(sent.id().toString());
        assertThat(observed.get(1).getLowCardinalityKeyValue("outcome").getValue()).isEqualTo("failed");
        assertThat(observed.get(1).getError()).hasMessage("boom");
    }

    @Test
    void backoffGrowsGeometricallyFromTheInitialDelay() {
        assertThat(SETTINGS.backoff(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(SETTINGS.backoff(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(SETTINGS.backoff(5)).isEqualTo(Duration.ofSeconds(16));
    }

    private static OutboxEvent event(String type, int attempts) {
        return new OutboxEvent(UUID.randomUUID(), OrderPlaced.AGGREGATE_TYPE, "ORD-" + UUID.randomUUID(), type, "{}", attempts);
    }

    /** An OrderPlaced handler that records what it was given and throws for the events told to fail. */
    private static final class FakeHandler implements OutboxEventHandler {
        private final List<OutboxEvent> handled = new ArrayList<>();
        private final Map<UUID, RuntimeException> failures = new HashMap<>();

        void failWith(OutboxEvent event, RuntimeException failure) {
            failures.put(event.id(), failure);
        }

        @Override
        public String eventType() {
            return OrderPlaced.EVENT_TYPE;
        }

        @Override
        public void handle(OutboxEvent event) {
            handled.add(event);
            var failure = failures.get(event.id());
            if (failure != null) {
                throw failure;
            }
        }
    }

    /** Collects the context of every stopped observation. */
    private record RecordingHandler(List<Observation.Context> stopped) implements ObservationHandler<Observation.Context> {
        @Override
        public void onStop(Observation.Context context) {
            stopped.add(context);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }
}
