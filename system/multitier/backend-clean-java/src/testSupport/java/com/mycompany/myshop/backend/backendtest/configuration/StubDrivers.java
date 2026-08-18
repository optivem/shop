package com.mycompany.myshop.backend.backendtest.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.clock.ClockStubDriver;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.ErpStubDriver;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.TaxStubDriver;

public final class StubDrivers {

    private StubDrivers() {
    }

    public static ErpStubDriver erp(WireMockServer server) {
        return new ErpStubDriver(clientFor(server));
    }

    public static TaxStubDriver tax(WireMockServer server) {
        return new TaxStubDriver(clientFor(server));
    }

    public static ClockStubDriver clock(WireMockServer server) {
        return new ClockStubDriver(clientFor(server));
    }

    private static WireMock clientFor(WireMockServer server) {
        return new WireMock("localhost", server.port());
    }
}
