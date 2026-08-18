package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import java.time.Instant;

public class SutClockReader {

    private final ClockGateway gateway;

    public SutClockReader(ClockGateway gateway) {
        this.gateway = gateway;
    }

    public Instant readTime() {
        return gateway.getCurrentTime();
    }
}
