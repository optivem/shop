package com.mycompany.myshop.backend.testkit.driver.port.external.clock;

/**
 * What the use case DSL needs from the Clock: the ability to program the time it reports. Sibling of
 * {@code ErpDriver} — see that port for why the vocabulary is stub-programming only.
 */
public interface ClockDriver {

    /** Liveness probe behind {@code assume().clock().shouldBeRunning()}. See {@code ErpDriver}. */
    void goToClock();

    void returnsTime(String isoInstant);

    void failsForTime(int status, String body);
}
