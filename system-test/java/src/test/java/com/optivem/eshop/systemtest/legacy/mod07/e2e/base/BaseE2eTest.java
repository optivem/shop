package com.optivem.eshop.systemtest.legacy.mod07.e2e.base;

import com.optivem.eshop.systemtest.legacy.mod07.base.BaseUseCaseDslTest;
import com.optivem.eshop.dsl.port.ExternalSystemMode;

public abstract class BaseE2eTest extends BaseUseCaseDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}




