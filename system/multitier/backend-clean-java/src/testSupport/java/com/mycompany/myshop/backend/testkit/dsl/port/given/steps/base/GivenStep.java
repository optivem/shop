package com.mycompany.myshop.backend.testkit.dsl.port.given.steps.base;

import com.mycompany.myshop.backend.testkit.dsl.port.given.GivenStage;
import com.mycompany.myshop.backend.testkit.dsl.port.then.ThenStage;
import com.mycompany.myshop.backend.testkit.dsl.port.when.WhenStage;

public interface GivenStep {
    GivenStage and();

    WhenStage when();

    ThenStage then();
}
