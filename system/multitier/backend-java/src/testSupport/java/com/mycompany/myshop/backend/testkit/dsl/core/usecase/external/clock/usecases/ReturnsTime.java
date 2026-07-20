package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;

public class ReturnsTime extends BaseClockUseCase {

    private String time;

    public ReturnsTime(ClockDriver driver) {
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
