package com.optivem.eshop.systemtest.legacy.mod06.e2e.base;

import com.optivem.eshop.systemtest.legacy.mod06.base.BaseChannelDriverTest;
import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;

public abstract class BaseE2eTest extends BaseChannelDriverTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



