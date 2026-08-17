package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;

/**
 * The Clock liveness probe behind {@code assume().clock().shouldBeRunning()}. Sibling of
 * {@code GoToErp} — see that use case for why it returns no {@code Result}.
 */
public class GoToClock extends BaseClockUseCase {

    public GoToClock(ClockDriver driver) {
        super(driver);
    }

    @Override
    public void execute() {
        driver.goToClock();
    }
}
