package com.optivem.eshop.systemtest.legacy.mod10.acceptance.base;

import com.optivem.eshop.systemtest.legacy.mod10.base.BaseScenarioDslTest;
import com.optivem.eshop.dsl.port.ExternalSystemMode;

public class BaseAcceptanceTest extends BaseScenarioDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}



