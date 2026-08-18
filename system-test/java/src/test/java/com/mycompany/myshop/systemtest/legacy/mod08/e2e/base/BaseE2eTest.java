package com.mycompany.myshop.systemtest.legacy.mod08.e2e.base;

import com.mycompany.myshop.systemtest.legacy.mod08.base.BaseScenarioDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public abstract class BaseE2eTest extends BaseScenarioDslTest {
    
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}




