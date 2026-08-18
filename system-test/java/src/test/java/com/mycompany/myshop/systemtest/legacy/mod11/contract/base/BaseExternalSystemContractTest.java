package com.mycompany.myshop.systemtest.legacy.mod11.contract.base;

import com.mycompany.myshop.systemtest.legacy.mod11.base.BaseScenarioDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseExternalSystemContractTest extends BaseScenarioDslTest {
    protected ScenarioDslImpl scenario;

    @BeforeEach
    void setUpScenario() {
        scenario = (ScenarioDslImpl) super.scenario;
    }

    @Override
    protected abstract ExternalSystemMode getFixedExternalSystemMode();
}

