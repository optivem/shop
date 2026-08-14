package com.mycompany.myshop.backend.contract.external.clock;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.clock.ClockStubDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@code Stub} side of the clock time contract. Arranges through the real
 * {@link ClockStubDriver}, so what {@link ClockRealParityContractTest} holds against the simulator is
 * the driver's own JSON — the artifact the parity pair exists to guard.
 *
 * <p>Needs no Docker of its own: the gateway is driven directly, with no Spring context and no
 * container.
 */
class ClockStubParityContractTest extends BaseClockTimeParityContractTest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    private ClockGateway clockGateway;

    @BeforeAll
    static void startWireMock() {
        WIRE_MOCK.start();
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void setUp() {
        WIRE_MOCK.resetAll();
        new ClockStubDriver(new WireMock(WIRE_MOCK.port())).returnsTime(PINNED_TIME);

        clockGateway = new ClockGateway();
        ReflectionTestUtils.setField(clockGateway, "clockUrl", WIRE_MOCK.baseUrl());
        ReflectionTestUtils.setField(clockGateway, "externalSystemMode", ExternalSystemMode.STUB.propertyValue());
    }

    @Override
    protected ClockGateway clockGateway() {
        return clockGateway;
    }
}
