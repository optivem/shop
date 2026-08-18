package com.mycompany.myshop.backend.contract.external.clock;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import java.time.Instant;
import org.junit.jupiter.api.Test;

abstract class BaseClockTimeParityContractTest {

    protected static final String PINNED_TIME = "2024-01-15T10:30:00.000Z";

    protected abstract ClockGateway clockGateway();

    @Test
    void getCurrentTimeReturnsTheAnnouncedTime() {
        assertThat(clockGateway().getCurrentTime()).isEqualTo(Instant.parse(PINNED_TIME));
    }
}
