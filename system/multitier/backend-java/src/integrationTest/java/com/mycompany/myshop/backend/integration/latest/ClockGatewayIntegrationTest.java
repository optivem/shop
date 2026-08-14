package com.mycompany.myshop.backend.integration.latest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.integration.latest.base.BaseGatewayIntegrationTest;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/**
 * Narrow-integration coverage for {@link ClockGateway}, the sibling of
 * {@link ErpGatewayIntegrationTest} and {@link TaxGatewayIntegrationTest}. Same shape: one gateway
 * driven directly against the in-process WireMock supplied by {@link BaseGatewayIntegrationTest},
 * every stub declared through the shared {@link ClockDsl} the component tests reach as
 * {@code app.clock()}.
 *
 * <p>This gateway carries a branch the other two do not: {@code external.system-mode} selects between
 * {@code Instant.now()} and the HTTP stub, and the component harness fixes it to {@code stub} for the
 * whole context. Here the class default is {@code stub} too, but the two tests below build their
 * gateway in an explicit mode instead — which lets the {@code real} and unknown-mode branches be
 * pinned. The {@code real} branch is what production runs on and is otherwise never exercised.
 *
 * <p>There is no {@code legacy/} counterpart, and that is a subset choice rather than an
 * impossibility — a raw-WireMock "before" for this gateway would be easy to write. It is skipped
 * because {@code legacy/} carries one twin per distinct contrast, not one per class (see
 * {@code component/legacy/CouponComponentTest}, whose twin exists to isolate the SUT-side contrast).
 * At this layer the sole contrast is inlined {@code WIRE_MOCK.stubFor} + JSON literal versus the
 * stub DSL, and {@code legacy/ErpGatewayIntegrationTest} already demonstrates it; a Clock twin would
 * repeat it verbatim against a different URL. Note this is a weaker claim than the one guarding
 * {@code contract/external/}, where a "before" would be genuinely circular.
 */
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
            .isInstanceOf(IllegalStateException.class)
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
