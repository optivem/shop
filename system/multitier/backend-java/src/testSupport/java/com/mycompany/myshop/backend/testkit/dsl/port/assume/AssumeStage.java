package com.mycompany.myshop.backend.testkit.dsl.port.assume;

import com.mycompany.myshop.backend.testkit.dsl.port.assume.steps.AssumeRunning;

/**
 * Preconditions the scenario assumes rather than arranges — liveness probes, mirroring the
 * system-test project's {@code AssumeStage} so the same scenario line reads identically at both
 * layers. {@link #myShop()} is the {@code GET /health} probe proving the in-process harness booted
 * and serves HTTP; {@link #erp()} / {@link #tax()} / {@link #clock()} probe the stub servers.
 *
 * <p><strong>What the external probes do and do not cover.</strong> They reach the stub through the
 * driver's own WireMock client, so they catch a driver pointed at a dead port. They do NOT catch a
 * driver pointed at the <em>wrong live</em> stub (e.g. {@code StubDrivers.erp(TAX)}), because
 * liveness is a predicate on one endpoint and that bug is a mismatch between two. Nor can they
 * report a stub that failed to start at all — the servers are started in {@code
 * AbstractComponentTest}'s static initializer, so that failure takes down every component test
 * including these.
 *
 * <p>Both of those gaps are covered by {@code component/latest/contract/*StubContractComponentTest},
 * which plant through the driver and read back through the SUT's production gateway. Those are the
 * load-bearing tests for stub wiring; these probes are a fast, legible first failure. Do not delete
 * the contract tests as redundant with these — the implication runs the other way.
 */
public interface AssumeStage {
    AssumeRunning myShop();

    AssumeRunning erp();

    AssumeRunning tax();

    AssumeRunning clock();
}
