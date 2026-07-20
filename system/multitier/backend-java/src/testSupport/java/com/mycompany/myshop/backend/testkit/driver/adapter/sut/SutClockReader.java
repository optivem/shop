package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import java.time.Instant;

/**
 * Reads the current time AS THE SUT SEES IT: a real HTTP call to the (stubbed) Clock URL plus the
 * SUT's own {@code GetTimeResponse} parse, delegating to the production {@link ClockGateway}. See
 * {@link SutErpReader} for why the read goes through the production gateway rather than a test-side
 * stub client.
 */
public class SutClockReader {

    private final ClockGateway gateway;

    public SutClockReader(ClockGateway gateway) {
        this.gateway = gateway;
    }

    public Instant readTime() {
        return gateway.getCurrentTime();
    }
}
