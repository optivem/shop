package com.optivem.eshop.systemtest.legacy.mod11.e2e.base;

import com.optivem.eshop.systemtest.legacy.mod11.base.BaseScenarioDslTest;
import com.optivem.eshop.dsl.port.ExternalSystemMode;

public class BaseE2eTest extends BaseScenarioDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}




