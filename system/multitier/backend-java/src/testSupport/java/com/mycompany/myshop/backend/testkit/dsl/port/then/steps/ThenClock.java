package com.mycompany.myshop.backend.support.port.then.steps;

import com.mycompany.myshop.backend.support.port.then.steps.base.ThenStep;

/**
 * The current time as the SUT sees it: read back through the SUT's production {@code ClockGateway}.
 * See {@link ThenProduct} for why the read goes through the production gateway.
 */
public interface ThenClock extends ThenStep<ThenClock> {
    ThenClock hasTime(String expectedTime);
}
