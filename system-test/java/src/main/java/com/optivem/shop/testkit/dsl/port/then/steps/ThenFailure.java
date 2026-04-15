package com.optivem.shop.testkit.dsl.port.then.steps;

import com.optivem.shop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenFailure extends ThenStep<ThenFailure> {
    ThenFailure errorMessage(String expectedMessage);

    ThenFailure fieldErrorMessage(String expectedField, String expectedMessage);
}

