package com.optivem.eshop.systemtest.legacy.mod06.e2e.base;

import com.optivem.eshop.systemtest.legacy.mod06.base.BaseChannelDriverTest;
import com.optivem.eshop.dsl.port.ExternalSystemMode;

import java.util.UUID;

public abstract class BaseE2eTest extends BaseChannelDriverTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }

    protected String createUniqueSku(String baseSku) {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return baseSku + "-" + suffix;
    }
}



