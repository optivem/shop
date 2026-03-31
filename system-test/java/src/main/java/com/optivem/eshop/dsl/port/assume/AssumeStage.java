package com.optivem.eshop.dsl.port.assume;

import com.optivem.eshop.dsl.port.assume.steps.AssumeRunning;

public interface AssumeStage {
    AssumeRunning shop();

    AssumeRunning erp();

    AssumeRunning clock();
}


