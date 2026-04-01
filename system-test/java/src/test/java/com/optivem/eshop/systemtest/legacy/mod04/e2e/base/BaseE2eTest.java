package com.optivem.eshop.systemtest.legacy.mod04.e2e.base;

import com.optivem.eshop.dsl.port.ExternalSystemMode;
import com.optivem.eshop.systemtest.legacy.mod04.base.BaseClientTest;

import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseClientTest {
    @BeforeEach
    void setUp() {
        setShopDriver();
        setUpExternalClients();
    }

    protected abstract void setShopDriver();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



