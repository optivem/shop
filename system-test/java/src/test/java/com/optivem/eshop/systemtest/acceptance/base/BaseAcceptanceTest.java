package com.optivem.eshop.systemtest.acceptance.base;

import com.optivem.eshop.systemtest.base.BaseScenarioDslTest;
import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;

public class BaseAcceptanceTest extends BaseScenarioDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}


