package com.mycompany.myshop.systemtest.legacy.mod07.e2e.base;

import com.mycompany.myshop.systemtest.legacy.mod07.base.BaseUseCaseDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public abstract class BaseE2eTest extends BaseUseCaseDslTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}




