package com.mycompany.myshop.backend.support.port.given.steps.base;

import com.mycompany.myshop.backend.support.port.given.GivenStage;
import com.mycompany.myshop.backend.support.port.then.ThenStage;
import com.mycompany.myshop.backend.support.port.when.WhenStage;

public interface GivenStep {
    GivenStage and();

    WhenStage when();

    ThenStage then();
}
