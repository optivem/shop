package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.FailsForTime;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.GoToClock;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.ReturnsTime;

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
