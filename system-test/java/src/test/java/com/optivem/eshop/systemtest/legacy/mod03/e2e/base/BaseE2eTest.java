package com.optivem.eshop.systemtest.legacy.mod03.e2e.base;

import com.optivem.eshop.dsl.port.ExternalSystemMode;
import com.optivem.eshop.systemtest.legacy.mod03.base.BaseRawTest;

import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseRawTest {
    @BeforeEach
    void setUp() {
        setShopDriver();
        setUpExternalHttpClients();
    }

    protected abstract void setShopDriver();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



