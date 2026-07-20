package com.mycompany.myshop.backend.support.port.then;

import com.mycompany.myshop.backend.support.port.then.steps.ThenFailure;
import com.mycompany.myshop.backend.support.port.then.steps.ThenSuccess;

/** The outcome of the scenario's action, plus everything {@link ThenStage} can already assert. */
public interface ThenResultStage extends ThenStage {
    ThenSuccess shouldSucceed();

    ThenFailure shouldFail();
}
