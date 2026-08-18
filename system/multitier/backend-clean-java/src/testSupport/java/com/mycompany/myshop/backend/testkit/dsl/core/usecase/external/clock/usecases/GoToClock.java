package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;

public class GoToClock extends BaseClockUseCase {

    public GoToClock(ClockDriver driver) {
        super(driver);
    }

    @Override
    public void execute() {
        driver.goToClock();
    }
}
