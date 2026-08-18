package com.mycompany.myshop.systemtest.legacy.mod10.acceptance.base;

import com.mycompany.myshop.systemtest.legacy.mod10.base.BaseScenarioDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public abstract class BaseAcceptanceTest extends BaseScenarioDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}



