package com.mycompany.myshop.backend.support.port.assume;

import com.mycompany.myshop.backend.support.port.assume.steps.AssumeRunning;

/**
 * Preconditions the scenario assumes rather than arranges: the {@code GET /health} probe that proves
 * the in-process harness booted and serves HTTP.
 *
 * <p>There is deliberately no {@code erp()} / {@code tax()} / {@code clock()} probe — those are
 * WireMock servers the test itself started, so a liveness check against them could never fail.
 */
public interface AssumeStage {
    AssumeRunning myShop();
}
