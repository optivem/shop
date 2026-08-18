package com.mycompany.myshop.backend.testkit.dsl.port.assume;

import com.mycompany.myshop.backend.testkit.dsl.port.assume.steps.AssumeRunning;

public interface AssumeStage {
    AssumeRunning myShop();

    AssumeRunning erp();

    AssumeRunning tax();

    AssumeRunning clock();
}
