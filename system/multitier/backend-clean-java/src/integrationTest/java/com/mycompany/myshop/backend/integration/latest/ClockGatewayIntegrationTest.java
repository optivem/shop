package com.mycompany.myshop.backend.integration.latest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.domain.gateways.ClockGatewayException;
import com.mycompany.myshop.backend.integration.latest.base.BaseGatewayIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class ClockGatewayIntegrationTest extends BaseGatewayIntegrationTest {

    @Test
    void getCurrentTimeReturnsStubbedTimeInStubMode() {
        clock().returnsTime().time("2026-03-10T12:00:00Z").execute();

        assertThat(clockGateway().getCurrentTime()).isEqualTo(Instant.parse("2026-03-10T12:00:00Z"));
    }

    @Test
    void getCurrentTimeThrowsOnServerError() {
        clock().failsForTime().status(500).body("Internal Server Error").execute();

        assertThatThrownBy(() -> clockGateway().getCurrentTime())
            .isInstanceOf(ClockGatewayException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getCurrentTimeIgnoresStubInRealMode() {
        clock().returnsTime().time("2026-03-10T12:00:00Z").execute();

        // The stub is programmed but must not be consulted: real mode reads the system clock.
        assertThat(clockGateway(ExternalSystemMode.REAL).getCurrentTime())
            .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
    }

    @Test
    void getCurrentTimeRejectsUnknownMode() {
        assertThatThrownBy(() -> clockGatewayWithRawMode("bogus").getCurrentTime())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bogus");
    }
}
