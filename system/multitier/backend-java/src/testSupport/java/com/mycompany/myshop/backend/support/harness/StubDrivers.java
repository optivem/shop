package com.mycompany.myshop.backend.support.harness;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Builds the external-system stub drivers from a running {@link WireMockServer}.
 *
 * <p>The drivers wrap a {@link WireMock} client rather than a server (see {@link ErpStubDriver}), so
 * every construction site repeated the same {@code new XStubDriver(new WireMock("localhost",
 * server.port()))} incantation — three times in the component harness and once per narrow-integration
 * test. This is that incantation, written once.
 *
 * <p>Serves both topologies: the component harness passes three separate servers (one per external
 * system, so a test can assert against each independently), a narrow-integration test passes the same
 * single server for whichever gateway it drives.
 */
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
