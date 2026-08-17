package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.usecases.base;

import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.BaseStubUseCase;

public abstract class BaseClockUseCase extends BaseStubUseCase<ClockDriver> {

    protected BaseClockUseCase(ClockDriver driver) {
        super(driver);
    }
}
