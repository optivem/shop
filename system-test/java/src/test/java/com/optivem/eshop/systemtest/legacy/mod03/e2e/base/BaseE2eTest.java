package com.optivem.eshop.systemtest.legacy.mod03.e2e.base;

import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;
import com.optivem.eshop.systemtest.legacy.mod03.base.BaseRawTest;

import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseRawTest {
    @BeforeEach
    void setUp() {
        setShopClient();
        setUpExternalHttpClients();
    }

    protected abstract void setShopClient();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



