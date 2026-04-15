package com.optivem.shop.testkit.dsl.core.usecase.external.clock.usecases;

import com.optivem.shop.testkit.driver.port.external.clock.ClockDriver;
import com.optivem.shop.testkit.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;

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
