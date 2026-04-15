package com.optivem.shop.systemtest.latest.contract.base;

import com.optivem.shop.systemtest.latest.base.BaseScenarioDslTest;
import com.optivem.shop.systemtest.configuration.ExternalSystemMode;
import com.optivem.shop.testkit.dsl.core.ScenarioDslImpl;
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
