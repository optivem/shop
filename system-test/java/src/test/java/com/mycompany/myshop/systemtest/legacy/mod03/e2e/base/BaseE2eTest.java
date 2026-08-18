package com.mycompany.myshop.systemtest.legacy.mod03.e2e.base;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import com.mycompany.myshop.systemtest.legacy.mod03.base.BaseRawTest;

import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2eTest extends BaseRawTest {
    @BeforeEach
    void setUp() {
        setMyShopClient();
        setUpExternalHttpClients();
    }

    protected abstract void setMyShopClient();

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



