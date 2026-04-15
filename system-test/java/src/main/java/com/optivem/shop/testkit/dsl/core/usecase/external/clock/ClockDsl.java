package com.optivem.shop.testkit.dsl.core.usecase.external.clock;

import com.optivem.shop.testkit.driver.port.external.clock.ClockDriver;
import com.optivem.shop.testkit.dsl.core.usecase.external.clock.usecases.GetTime;
import com.optivem.shop.testkit.dsl.core.usecase.external.clock.usecases.GoToClock;
import com.optivem.shop.testkit.dsl.core.usecase.external.clock.usecases.ReturnsTime;
import com.optivem.shop.testkit.common.Closer;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;

public class ClockDsl implements AutoCloseable {
    protected final ClockDriver driver;
    protected final UseCaseContext context;

    public ClockDsl(ClockDriver driver, UseCaseContext context) {
        this.driver = driver;
        this.context = context;
    }

    @Override
    public void close() {
        Closer.close(driver);
    }

    public GoToClock goToClock() {
        return new GoToClock(driver, context);
    }

    public ReturnsTime returnsTime() {
        return new ReturnsTime(driver, context);
    }

    public GetTime getTime() {
        return new GetTime(driver, context);
    }
}
