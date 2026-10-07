package com.mycompany.myshop.systemtest.latest.acceptance.base;

import com.mycompany.myshop.systemtest.configuration.Configuration;
import com.mycompany.myshop.systemtest.latest.base.BaseScenarioDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public abstract class BaseAcceptanceTest extends BaseScenarioDslTest {
    @Override
    protected Configuration loadConfiguration() {
        var configuration = super.loadConfiguration();
        requireKeycloakBaseUrl(configuration);
        return configuration;
    }

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}
