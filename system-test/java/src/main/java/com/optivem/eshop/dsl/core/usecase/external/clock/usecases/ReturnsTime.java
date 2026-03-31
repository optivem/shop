package com.optivem.eshop.dsl.core.usecase.external.clock.usecases;

import com.optivem.eshop.dsl.driver.port.external.clock.ClockDriver;
import com.optivem.eshop.dsl.driver.port.external.clock.dtos.ReturnsTimeRequest;
import com.optivem.eshop.dsl.core.usecase.external.clock.usecases.base.BaseClockUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.core.shared.VoidVerification;

public class ReturnsTime extends BaseClockUseCase<Void, VoidVerification> {
    private String time;

    public ReturnsTime(ClockDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsTime time(String time) {
        this.time = time;
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = ReturnsTimeRequest.builder()
                .time(time)
                .build();

        var result = driver.returnsTime(request);

        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
