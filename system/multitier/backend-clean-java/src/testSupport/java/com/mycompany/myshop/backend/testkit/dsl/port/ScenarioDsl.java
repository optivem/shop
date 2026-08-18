package com.mycompany.myshop.backend.testkit.dsl.port;

import com.mycompany.myshop.backend.testkit.dsl.port.assume.AssumeStage;
import com.mycompany.myshop.backend.testkit.dsl.port.given.GivenStage;
import com.mycompany.myshop.backend.testkit.dsl.port.when.WhenStage;

public interface ScenarioDsl {
    AssumeStage assume();

    GivenStage given();

    WhenStage when();
}
