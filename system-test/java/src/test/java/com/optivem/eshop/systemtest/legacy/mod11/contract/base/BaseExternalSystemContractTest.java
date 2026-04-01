package com.optivem.eshop.systemtest.legacy.mod11.contract.base;

import com.optivem.eshop.systemtest.legacy.mod11.base.BaseScenarioDslTest;
import com.optivem.eshop.dsl.port.ExternalSystemMode;
import com.optivem.eshop.dsl.core.ScenarioDslImpl;
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

