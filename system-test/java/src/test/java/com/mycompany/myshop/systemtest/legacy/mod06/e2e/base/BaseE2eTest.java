package com.mycompany.myshop.systemtest.legacy.mod06.e2e.base;

import com.mycompany.myshop.systemtest.legacy.mod06.base.BaseChannelDriverTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public abstract class BaseE2eTest extends BaseChannelDriverTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



