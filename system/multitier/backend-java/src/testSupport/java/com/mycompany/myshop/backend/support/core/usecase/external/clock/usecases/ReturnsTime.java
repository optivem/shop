package com.mycompany.myshop.backend.support.core.usecase.external.clock.usecases;

import com.mycompany.myshop.backend.support.harness.ClockStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.clock.usecases.base.BaseClockUseCase;

public class ReturnsTime extends BaseClockUseCase {

    private String time;

    public ReturnsTime(ClockStubDriver driver) {
        super(driver);
    }

    public ReturnsTime time(String isoInstant) {
        this.time = isoInstant;
        return this;
    }

    @Override
    public void execute() {
        driver.stubTime(time);
    }
}
