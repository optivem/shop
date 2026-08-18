package com.mycompany.myshop.backend.testkit.dsl.port.then;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenFailure;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenSuccess;

public interface ThenResultStage extends ThenStage {
    ThenSuccess shouldSucceed();

    ThenFailure shouldFail();
}
