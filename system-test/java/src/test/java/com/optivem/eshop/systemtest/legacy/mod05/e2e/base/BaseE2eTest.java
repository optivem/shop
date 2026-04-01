package com.optivem.eshop.systemtest.legacy.mod05.e2e.base;

import com.optivem.eshop.systemtest.legacy.mod05.base.BaseDriverTest;
import com.optivem.eshop.systemtest.configuration.ExternalSystemMode;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseDriverTest {
    @BeforeEach
    void setUpDrivers() {
        setShopDriver();
        setUpExternalDrivers();
    }

    protected abstract void setShopDriver();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}