package com.mycompany.myshop.systemtest.legacy.mod04.e2e.base;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import com.mycompany.myshop.systemtest.legacy.mod04.base.BaseClientTest;

import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseClientTest {
    @BeforeEach
    void setUp() {
        setMyShopClient();
        setUpExternalClients();
    }

    protected abstract void setMyShopClient();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



