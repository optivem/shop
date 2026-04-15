package com.optivem.shop.testkit.dsl.port.assume;

import com.optivem.shop.testkit.dsl.port.assume.steps.AssumeRunning;

public interface AssumeStage {
    AssumeRunning shop();

    AssumeRunning erp();

    AssumeRunning tax();

    AssumeRunning clock();
}
