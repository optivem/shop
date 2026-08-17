package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.FailsForTime;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.GoToClock;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.ReturnsTime;

/**
 * The Clock, as the component test sees it — the reason time is controllable at all.
 *
 * <pre>{@code
 * app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
 * app.clock().failsForTime().status(500).body("Internal Server Error").execute();
 * }</pre>
 */
public class ClockDsl {

    private final ClockDriver driver;

    public ClockDsl(ClockDriver driver) {
        this.driver = driver;
    }

    public GoToClock goToClock() {
        return new GoToClock(driver);
    }

    public ReturnsTime returnsTime() {
        return new ReturnsTime(driver);
    }

    public FailsForTime failsForTime() {
        return new FailsForTime(driver);
    }
}
