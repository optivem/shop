package com.mycompany.myshop.backend.backendtest.configuration;

/**
 * Typed form of the {@code external.system-mode} property the SUT reads — the mode the *system under
 * test* runs in, not a choice about which test-side driver programs the external system. Only
 * {@code ClockGateway} branches on it today ({@code Instant.now()} vs the HTTP stub); ERP and Tax
 * always go over HTTP.
 *
 * <p>Exists to keep the bare {@code "stub"} / {@code "real"} string literals out of the test layers:
 * they were repeated across the component harness and the narrow-integration tests, where a typo
 * silently lands in the gateway's unknown-mode branch rather than failing to compile.
 *
 * <p>Lives in {@code testSupport} so the component, contract, and narrow-integration layers share one
 * definition — the same reason the stub drivers and use case DSL live here. Sits under
 * {@code configuration/} rather than {@code harness/}: it configures how a layer is wired, it is not
 * itself a driver.
 */
public enum ExternalSystemMode {

    /** The gateway talks HTTP to a stubbed external system. What every test layer runs on. */
    STUB,

    /**
     * The gateway uses its production non-HTTP path (for {@code ClockGateway}, the system clock).
     * Deliberately not wired to a real external service at these layers: stub-vs-real against live
     * systems stays a system-test concern, so nothing here needs live infrastructure to run.
     */
    REAL;

    /** The literal the SUT compares against, e.g. {@code "stub"}. */
    public String propertyValue() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
