package com.optivem.eshop.systemtest.legacy.mod04.e2e.base;

import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;
import com.optivem.eshop.systemtest.legacy.mod04.base.BaseClientTest;

import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseClientTest {
    @BeforeEach
    void setUp() {
        setShopClient();
        setUpExternalClients();
    }

    protected abstract void setShopClient();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



