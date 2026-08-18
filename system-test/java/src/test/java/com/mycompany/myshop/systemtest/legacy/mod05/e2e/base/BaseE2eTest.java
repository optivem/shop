package com.mycompany.myshop.systemtest.legacy.mod05.e2e.base;

import com.mycompany.myshop.systemtest.legacy.mod05.base.BaseDriverTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseDriverTest {
    @BeforeEach
    void setUpDrivers() {
        setMyShopDriver();
        setUpExternalDrivers();
    }

    protected abstract void setMyShopDriver();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}