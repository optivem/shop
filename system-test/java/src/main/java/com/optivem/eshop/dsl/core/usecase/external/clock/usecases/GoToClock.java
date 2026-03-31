package com.optivem.eshop.dsl.core.usecase.external.clock.usecases;

import com.optivem.eshop.dsl.driver.port.external.clock.ClockDriver;
import com.optivem.eshop.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.core.shared.VoidVerification;

public class GoToClock extends BaseClockUseCase<Void, VoidVerification> {
    public GoToClock(ClockDriver clockDriver, UseCaseContext useCaseContext) {
        super(clockDriver, useCaseContext);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.goToClock();
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
