package com.optivem.shop.dsl.port.assume;

import com.optivem.shop.dsl.port.assume.steps.AssumeRunning;

public interface AssumeStage {
    AssumeRunning shop();

    AssumeRunning erp();

    AssumeRunning tax();

    AssumeRunning clock();
}
