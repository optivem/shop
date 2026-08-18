package com.mycompany.myshop.systemtest.latest.e2e.base;

import com.mycompany.myshop.systemtest.latest.base.BaseScenarioDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public abstract class BaseE2eTest extends BaseScenarioDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}




