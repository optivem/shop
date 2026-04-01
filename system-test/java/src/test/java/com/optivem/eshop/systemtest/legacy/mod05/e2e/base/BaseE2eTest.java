package com.optivem.eshop.systemtest.legacy.mod05.e2e.base;

import com.optivem.eshop.systemtest.legacy.mod05.base.BaseDriverTest;
import com.optivem.eshop.dsl.port.ExternalSystemMode;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

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

    protected String createUniqueSku(String baseSku) {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return baseSku + "-" + suffix;
    }
}



