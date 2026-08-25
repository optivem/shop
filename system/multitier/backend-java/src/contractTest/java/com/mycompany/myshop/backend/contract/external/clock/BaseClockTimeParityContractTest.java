package com.mycompany.myshop.backend.contract.external.clock;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Shared shape for the clock time contract: does the production {@link ClockGateway}'s parse still
 * agree with whatever is answering at {@link #clockGateway()}'s URL?
 * {@link ClockStubParityContractTest} answers that against {@code ClockStubDriver}'s JSON;
 * {@link ClockRealParityContractTest} answers it against the real clock simulator.
 *
 * <p><strong>Unarranged, unlike the ERP and tax twins.</strong> The simulator's {@code /clock/api/time}
 * is a hardcoded handler, not a mutable resource — there is nothing to provision — so parity is
 * pinned the other way round: {@link #PINNED_TIME} is the value the simulator announces, and the stub
 * side is required to announce the same thing. That still catches the drift the pair exists for (a
 * renamed field, a changed timestamp format, a changed value); it just cannot vary the fixture.
 *
 * <p>{@code ClockGetTimeResponse} carries {@code @JsonIgnoreProperties(ignoreUnknown = true)}, so an
 * additive change on the clock side is tolerated by design and will not fail this test — only a
 * change to {@code time} itself will.
 */
abstract class BaseClockTimeParityContractTest {

    /** The instant {@code external-systems/simulators/mock-server.js} serves from {@code /clock/api/time}. */
    protected static final String PINNED_TIME = "2024-01-15T10:30:00.000Z";

    protected abstract ClockGateway clockGateway();

    @Test
    void getCurrentTimeReturnsTheAnnouncedTime() {
        assertThat(clockGateway().getCurrentTime()).isEqualTo(Instant.parse(PINNED_TIME));
    }
}
