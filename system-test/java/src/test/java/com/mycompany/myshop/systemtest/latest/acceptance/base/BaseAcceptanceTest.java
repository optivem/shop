package com.mycompany.myshop.systemtest.latest.acceptance.base;

import com.mycompany.myshop.systemtest.latest.base.BaseScenarioDslTest;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KeycloakRequirementExtension.class)
public abstract class BaseAcceptanceTest extends BaseScenarioDslTest {
    boolean isKeycloakConfigured() {
        var keycloakBaseUrl = loadConfiguration().getKeycloakBaseUrl();
        return keycloakBaseUrl != null && !keycloakBaseUrl.isBlank();
    }

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}
