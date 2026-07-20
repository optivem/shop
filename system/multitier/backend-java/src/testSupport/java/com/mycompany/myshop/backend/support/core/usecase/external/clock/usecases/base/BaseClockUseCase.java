package com.mycompany.myshop.backend.support.core.usecase.external.clock.usecases.base;

import com.mycompany.myshop.backend.support.harness.ClockStubDriver;
import com.mycompany.myshop.backend.support.core.shared.BaseStubUseCase;

public abstract class BaseClockUseCase extends BaseStubUseCase<ClockStubDriver> {

    protected BaseClockUseCase(ClockStubDriver driver) {
        super(driver);
    }
}
