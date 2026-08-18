package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;

public class FailsForTime extends BaseClockUseCase {

    private int status;
    private String body;

    public FailsForTime(ClockDriver driver) {
        super(driver);
    }

    public FailsForTime status(int status) {
        this.status = status;
        return this;
    }

    public FailsForTime body(String body) {
        this.body = body;
        return this;
    }

    @Override
    public void execute() {
        driver.failsForTime(status, body);
    }
}
